package com.fu.fuaicode.config;

import com.fu.fuaicode.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Slf4j
public class ScreenshotConfig {

    /**
     * 每天凌晨2点清理临时截图文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanTempScreenshotFiles() {
        log.info("开始清理临时截图文件");
        try {
            WebScreenshotUtils.cleanupLocalFile();
        }catch (Exception e){
            log.error("清理临时截图文件失败", e);
        }
    }
}
