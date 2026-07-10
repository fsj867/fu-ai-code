package com.fu.fuaicode.langgraph4j.state;

import com.fu.fuaicode.model.QualityResult;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Sinks;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工作流上下文 - 存储所有状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext implements Serializable {

    /**
     * WorkflowContext 在 MessagesState 中的存储key
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 用户原始输入的提示词
     */
    private String originalPrompt;

    /**
     * 图片资源字符串
     */
    private String imageListStr;

    /**
     * 图片资源列表
     */
    private List<ImageResource> imageList;

    /**
     * 增强后的提示词
     */
    private String enhancedPrompt;

    /**
     * 代码生成类型
     */
    private CodeGenTypeEnum generationType;

    /**
     * 生成的代码目录
     */
    private String generatedCodeDir;

    /**
     * 构建成功的目录
     */
    private String buildResultDir;

    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 质量检查结果
     */
    private QualityResult qualityResult;

    /**
     * 代码生成重试次数（用于限制最大重试次数，避免无限循环）
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_COUNT = 3;

    /**
     * 流式事件发射器（不参与序列化，用于运行时转发工作流事件流）
     */
    private transient Object eventSink;

    /**
     * ThreadLocal 方式传递事件发射器，避免状态序列化导致引用丢失
     */
    private static final ThreadLocal<Object> EVENT_SINK_HOLDER = new ThreadLocal<>();

    public static void setEventSinkHolder(Object sink) {
        EVENT_SINK_HOLDER.set(sink);
    }

    public static Object getEventSinkHolder() {
        return EVENT_SINK_HOLDER.get();
    }

    public static void clearEventSinkHolder() {
        EVENT_SINK_HOLDER.remove();
    }


    @Serial
    private static final long serialVersionUID = 1L;

    // ========== 上下文操作方法 ==========

    /**
     * 从 MessagesState 中获取 WorkflowContext
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * 将 WorkflowContext 保存到 MessagesState 中
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}
