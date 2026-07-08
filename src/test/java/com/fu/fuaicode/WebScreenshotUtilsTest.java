package com.fu.fuaicode;

import com.fu.fuaicode.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;

@Slf4j
@SpringBootTest
public class WebScreenshotUtilsTest {
    @Resource
    private WebScreenshotUtils webScreenshotUtils;
    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://www.codefather.cn";
        CompletableFuture<String> stringCompletableFuture = webScreenshotUtils.takeScreenshotAsync(testUrl);
        String webPageScreenshot = stringCompletableFuture.join();
        Assertions.assertNotNull(webPageScreenshot);
    }
}
