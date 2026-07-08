package com.fu.fuaicode.ai;
import com.fu.fuaicode.ai.guardrail.PromptSafetyInputGuardrail;
import com.fu.fuaicode.ai.tools.FileWriteTool;
import com.fu.fuaicode.exception.BusinessException;
import com.fu.fuaicode.exception.ErrorCode;
import com.fu.fuaicode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fu.fuaicode.service.ChatHistoryService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 工厂模式初始化AI服务
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel dashScopeChatModel;
    @Resource
    private StreamingChatModel dashScopeStreamingChatModel;
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 根据appId获得服务
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("根据appId获得服务，appId={{}",appId);
        //根据appId获得独立的对话记忆
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
                //加载对话历史到内存
                chatHistoryService.loadChatHistortToMemory(appId,messageWindowChatMemory,20);
        return switch (codeGenType) {
            //原生模式
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(dashScopeChatModel)
                    .streamingChatModel(dashScopeStreamingChatModel)
                    .chatMemory(messageWindowChatMemory)
                    .inputGuardrails(new PromptSafetyInputGuardrail()) // 输入安全防护
                    .build();
            //VUE工程模式
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatModel(dashScopeChatModel)
                    .chatMemoryProvider(memoryId -> messageWindowChatMemory) //为memoryId绑定会话记忆
                    .tools(new FileWriteTool())
                    .inputGuardrails(new PromptSafetyInputGuardrail()) // 输入安全防护
                    .hallucinatedToolNameStrategy(
                            toolExecutionRequest -> ToolExecutionResultMessage.from(
                                    toolExecutionRequest, "Error: there is no tool called" + toolExecutionRequest.name()
                            )
                    ).build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型"+codeGenType.getValue());
        };
    }


    public AiCodeGeneratorService aiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return getAiCodeGeneratorService(appId,codeGenType);
    }
}
