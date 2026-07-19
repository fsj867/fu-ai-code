package com.fu.fuaicode.langgraph4j;

import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.exception.ThrowUtils;
import com.fu.fuaicode.langgraph4j.node.*;
import com.fu.fuaicode.langgraph4j.state.WorkflowContext;
import com.fu.fuaicode.langgraph4j.state.WorkflowEvent;
import com.fu.fuaicode.model.QualityResult;
import com.fu.fuaicode.model.entity.App;
import com.fu.fuaicode.model.entity.User;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import com.fu.fuaicode.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
@Component
public class CodeGenWorkflow {

    @Resource
    private AppService appService;

    // 创建编译图
    public CompiledGraph<MessagesState<String>> createGraph(){
        try {
            return new MessagesStateGraph<String>()
                    // nodes
                    .addNode("image_collector",ImageCollectorNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    // edges
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of("build", "project_builder",
                                    "skip_build", END,
                                    "fail", "code_generator"
                            ))
                    .addEdge("project_builder", END)
                    // compile
                    .compile();


        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建编译图失败: " + e.getMessage());
        }
    }
    // 执行图 
    public WorkflowContext execute(String prompt) {
        CompiledGraph<MessagesState<String>> graph = createGraph();
        // 初始状态
        WorkflowContext inintialState = WorkflowContext.builder()
                .currentStep("初始化")
                .originalPrompt(prompt)
                .build();
        GraphRepresentation graphRepresentation = graph.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("图结构：{}", graphRepresentation.content());

        int currentStep = 1;
        WorkflowContext finalState = null;
        for (NodeOutput<MessagesState<String>> step : graph.stream(Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, inintialState))){
            log.info("第 {} 步: {}", currentStep, step.node());
            // 获取当前状态
            WorkflowContext workflowContext = WorkflowContext.getContext(step.state());
            if (workflowContext != null) {
                log.info("当前状态: {}", workflowContext);
                finalState = workflowContext;
            }
            currentStep++;
        }
        log.info("最终状态: {}", finalState);
        return finalState;
    }

    private static final long SSE_TIMEOUT_MS = 600000L;

    // 执行图，流式输出（SseEmitter 方式，Spring MVC 原生支持，最稳定）
    public SseEmitter executeSse(String prompt, Long appId, User loginUser) {
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "用户没有权限访问应用");

        CodeGenTypeEnum appGenType = CodeGenTypeEnum.getByValue(app.getCodeGenType());
        ThrowUtils.throwIf(appGenType == null, ErrorCode.PARAMS_ERROR, "应用代码生成类型无效");

        CompiledGraph<MessagesState<String>> graph = createGraph();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        SseEmitterAdapter emitterAdapter = new SseEmitterAdapter(emitter);

        WorkflowContext inintialState = WorkflowContext.builder()
                .currentStep("初始化")
                .originalPrompt(prompt)
                .appId(appId)
                .generationType(appGenType)
                .eventSink(emitterAdapter)
                .build();
        GraphRepresentation graphRepresentation = graph.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("图结构：{}", graphRepresentation.content());

        emitter.onCompletion(() -> {
            log.info("SSE 连接正常关闭");
            WorkflowContext.clearEventSinkHolder();
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            WorkflowEvent timeoutEvent = new WorkflowEvent();
            timeoutEvent.setEventType(WorkflowEvent.TYPE_ERROR);
            timeoutEvent.setContent("连接超时");
            emitterAdapter.tryEmitNext(timeoutEvent);
            WorkflowContext.clearEventSinkHolder();
        });

        emitter.onError(e -> {
            log.error("SSE 连接异常", e);
            WorkflowContext.clearEventSinkHolder();
        });

        Thread.startVirtualThread(() -> {
            try {
                WorkflowContext.setEventSinkHolder(emitterAdapter);
                int currentStep = 1;
                for (NodeOutput<MessagesState<String>> step : graph.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, inintialState))) {
                    String nodeName = step.node();
                    if (nodeName.startsWith("__") && nodeName.endsWith("__")) {
                        continue;
                    }
                    log.info("第 {} 步完成: {}", currentStep, nodeName);
                    currentStep++;
                }
                log.info("工作流执行完成，发射完成事件");
                WorkflowEvent completeEvent = new WorkflowEvent();
                completeEvent.setEventType(WorkflowEvent.TYPE_COMPLETE);
                sendSseEvent(emitter, completeEvent);
                emitter.complete();
                log.info("事件流发射完毕");
            } catch (Exception e) {
                log.error("工作流执行异常", e);
                WorkflowEvent errorEvent = new WorkflowEvent();
                errorEvent.setEventType(WorkflowEvent.TYPE_ERROR);
                errorEvent.setContent(e.getMessage() != null ? e.getMessage() : "工作流执行异常");
                try {
                    sendSseEvent(emitter, errorEvent);
                    emitter.complete();
                } catch (Exception ex) {
                    log.warn("发送错误事件失败", ex);
                }
            } finally {
                WorkflowContext.clearEventSinkHolder();
            }
        });

        return emitter;
    }

    private void sendSseEvent(SseEmitter emitter, WorkflowEvent event) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("data", cn.hutool.json.JSONUtil.toJsonStr(event));
            String jsonStr = cn.hutool.json.JSONUtil.toJsonStr(data);
            emitter.send(SseEmitter.event().data(jsonStr));
        } catch (Exception e) {
            log.warn("发送 SSE 事件失败", e);
        }
    }

    /**
     * SseEmitter 适配器，供 CodeGeneratorNode 等节点发射 token 事件
     */
    public static class SseEmitterAdapter {
        private final SseEmitter emitter;

        public SseEmitterAdapter(SseEmitter emitter) {
            this.emitter = emitter;
        }

        public void tryEmitNext(WorkflowEvent event) {
            if (emitter != null) {
                try {
                    Map<String, String> data = new HashMap<>();
                    data.put("data", cn.hutool.json.JSONUtil.toJsonStr(event));
                    String jsonStr = cn.hutool.json.JSONUtil.toJsonStr(data);
                    emitter.send(SseEmitter.event().data(jsonStr));
                } catch (Exception e) {
                    log.warn("发送 SSE 事件失败", e);
                }
            }
        }
    }

    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        // 跳过构建
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        // VUE 需要构建
        return "build";
    }

    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        // 如果质检失败，检查重试次数
        if (qualityResult == null || !qualityResult.getIsValid()) {
            int retryCount = context.getRetryCount() != null ? context.getRetryCount() : 0;
            if (retryCount >= WorkflowContext.MAX_RETRY_COUNT) {
                log.warn("代码质检已达最大重试次数 ({})，不再重试，继续后续流程", WorkflowContext.MAX_RETRY_COUNT);
                return routeBuildOrSkip(state);
            }
            log.error("代码质检失败，第 {} 次重试生成代码", retryCount + 1);
            context.setRetryCount(retryCount + 1);
            return "fail";
        }
        // 质检通过，使用原有的构建路由逻辑
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }

}
