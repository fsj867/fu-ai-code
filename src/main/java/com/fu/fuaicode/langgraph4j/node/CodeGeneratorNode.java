package com.fu.fuaicode.langgraph4j.node;

import cn.hutool.core.io.FileUtil;
import com.fu.fuaicode.constant.AppConstant;
import com.fu.fuaicode.core.AiCodeGeneratorFacade;
import com.fu.fuaicode.langgraph4j.SpringContextUtil;
import com.fu.fuaicode.langgraph4j.state.WorkflowContext;
import com.fu.fuaicode.langgraph4j.state.WorkflowEvent;
import com.fu.fuaicode.model.QualityResult;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class CodeGeneratorNode {

    /**
     * 统一发射事件，支持 Sinks.Many 和 FluxSink 适配器
     */
    private static void emitEvent(Object sink, WorkflowEvent event) {
        if (sink == null) {
            return;
        }
        try {
            if (sink instanceof reactor.core.publisher.Sinks.Many) {
                ((reactor.core.publisher.Sinks.Many<WorkflowEvent>) sink).tryEmitNext(event);
            } else {
                // 尝试调用 tryEmitNext 方法（适配器模式）
                java.lang.reflect.Method method = sink.getClass().getMethod("tryEmitNext", WorkflowEvent.class);
                method.invoke(sink, event);
            }
        } catch (Exception e) {
            log.warn("发射事件失败", e);
        }
    }

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 代码生成");
            // 构造用户消息，如果存在质检失败结果则添加错误修复信息
            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();
            AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            log.info("开始生成代码，类型: {} ({})", generationType.getValue(), generationType.getValue());
            Long appId = context.getAppId() != null ? context.getAppId() : 0L;
            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);

            // 如果是重试（已有质检失败结果），清理旧的 dist 目录，避免构建节点跳过
            QualityResult qualityResult = context.getQualityResult();
            if (isQualityCheckFailed(qualityResult)) {
                File distDir = new File(generatedCodeDir, "dist");
                if (distDir.exists() && distDir.isDirectory()) {
                    log.info("检测到重试，清理旧的 dist 目录: {}", distDir.getAbsolutePath());
                    FileUtil.del(distDir);
                }
            }

            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(userMessage, generationType, appId);
            // 监听代码生成流，将 token 发送至事件流（优先从 ThreadLocal 获取，避免状态序列化丢失引用）
            Object eventSinkObj = WorkflowContext.getEventSinkHolder();
            if (eventSinkObj == null) {
                eventSinkObj = context.getEventSink();
            }
            if (eventSinkObj != null) {
                final Object finalSinkObj = eventSinkObj;
                codeStream = codeStream.doOnNext(chunk -> {
                    WorkflowEvent event = new WorkflowEvent();
                    event.setEventType(WorkflowEvent.TYPE_TOKEN);
                    event.setStepName("code_generator");
                    event.setContent(chunk);
                    emitEvent(finalSinkObj, event);
                });
            }
            // 同步等待流式输出完成
            codeStream.blockLast(Duration.ofMinutes(10));
            log.info("AI 代码生成完成，生成目录: {}", generatedCodeDir);
            // 设置当前步骤为代码生成
            context.setCurrentStep("代码生成");
            context.setGeneratedCodeDir(generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }
    /**
     * 构造用户消息，如果存在质检失败结果则添加错误修复信息
     */
    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();
        // 检查是否存在质检失败结果
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            // 直接将错误修复信息作为新的提示词（起到了修改的作用）
            userMessage = buildErrorFixPrompt(qualityResult);
        }
        return userMessage;
    }

    /**
     * 判断质检是否失败
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    /**
     * 构造错误修复提示词
     */
    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
        // 添加错误列表
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        // 添加修复建议（如果有）
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n请根据上述问题和建议重新生成代码，确保修复所有提到的问题。");
        return errorInfo.toString();
    }

}
