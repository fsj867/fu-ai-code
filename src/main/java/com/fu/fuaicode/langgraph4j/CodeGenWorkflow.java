package com.fu.fuaicode.langgraph4j;

import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.langgraph4j.node.*;
import com.fu.fuaicode.langgraph4j.state.WorkflowContext;
import com.fu.fuaicode.model.QualityResult;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class CodeGenWorkflow {

    // 创建编译图
    public CompiledGraph<MessagesState<String>> createGraph(){
        try {
            return new MessagesStateGraph<String>()
                    // nodes
                    .addNode("image_collector",ImageCollectorNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    // edges
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addConditionalEdges("code_generator",
                            edge_async(this::routeBuildOrSkip),
                            Map.of("build", "project_builder", //质检通过，需要构建
                                    "skip_build", END, //质检通过，不需要构建
                                    "fail", "code_generator" //质检不通过，重新生成代码
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
        // 如果质检失败，重新生成代码
        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return "fail";
        }
        // 质检通过，使用原有的构建路由逻辑
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }

}
