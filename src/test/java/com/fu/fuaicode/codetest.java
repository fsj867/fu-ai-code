package com.fu.fuaicode;

import com.fu.fuaicode.ai.AiCodeGeneratorService;
import com.fu.fuaicode.ai.model.HtmlCodeResult;
import com.fu.fuaicode.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class codetest {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    public void test() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("帮我做一个记账小工具,不超过20行");
        Assertions.assertNotNull(result);
    }
    @Test
    public void test2() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("帮我写一个登录页面");
        Assertions.assertNotNull(result);
    }


}
