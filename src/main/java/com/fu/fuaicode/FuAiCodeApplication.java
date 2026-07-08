package com.fu.fuaicode;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import dev.langchain4j.openai.spring.AutoConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
@EnableCaching
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class, AutoConfig.class})
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.fu.fuaicode.mapper")
public class FuAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuAiCodeApplication.class, args);
    }

}
