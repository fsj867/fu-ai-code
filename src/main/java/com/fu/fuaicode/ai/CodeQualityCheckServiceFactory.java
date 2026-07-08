package com.fu.fuaicode.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;

public class CodeQualityCheckServiceFactory {

    @Resource
    private ChatModel dashScopeChatModel;

    @Bean
    public CodeQualityCheckService codeQualityCheckService() {
        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(dashScopeChatModel)
                .build();
    }

}
