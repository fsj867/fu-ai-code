package com.fu.fuaicode.ai;

import com.fu.fuaicode.ai.tools.ImageSearchTool;
import com.fu.fuaicode.ai.tools.LogoGeneratorTool;
import com.fu.fuaicode.ai.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel dashScopeChatModel;
    @Resource
    private ImageSearchTool imageSearchTool;
    @Resource
    private LogoGeneratorTool logoGeneratorTool;
    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Bean
    public ImageCollectionService ImageCollectionService() {

        return AiServices.builder(ImageCollectionService.class)
                .chatModel(dashScopeChatModel)
                .tools(imageSearchTool, logoGeneratorTool, undrawIllustrationTool)
                .build();
    }
}
