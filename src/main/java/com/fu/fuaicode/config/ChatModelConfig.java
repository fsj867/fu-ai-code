package com.fu.fuaicode.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * AI模型配置
 *
 */
@Configuration
public class ChatModelConfig {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String chatBaseUrl;
    
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String chatApiKey;
    
    @Value("${langchain4j.open-ai.chat-model.model-name:qwen-turbo}")
    private String chatModelName;
    
    @Value("${langchain4j.open-ai.chat-model.max-tokens:8192}")
    private Integer chatMaxTokens;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.base-url}")
    private String streamingBaseUrl;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.api-key}")
    private String streamingApiKey;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.model-name:qwen-turbo}")
    private String streamingModelName;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.max-tokens:8192}")
    private Integer streamingMaxTokens;

    /**
     * 普通对话模型（用于HTML和多文件生成）
     */
    @Bean
    @Scope("prototype")
    public ChatModel dashScopeChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .maxTokens(chatMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 流式对话模型（用于HTML和多文件生成）
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel dashScopeStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(streamingBaseUrl)
                .apiKey(streamingApiKey)
                .modelName(streamingModelName)
                .maxTokens(streamingMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 推理模型流式专用（用于vue项目生成，带工具调用）
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(streamingBaseUrl)
                .apiKey(streamingApiKey)
                .modelName(streamingModelName)
                .maxTokens(streamingMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
