package com.fu.fuaicode.mq;

import com.fu.fuaicode.core.builder.VueProjectBuilder;
import com.fu.fuaicode.model.entity.App;
import com.fu.fuaicode.service.AppService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class BuildConsumer {

    @Autowired
    private VueProjectBuilder vueProjectBuilder;

    @Autowired
    private AppService appService;

    @RabbitListener(queues = MqConstant.QUEUE_NAME)
    public void handleBuildTask(BuildTask task, Message message, Channel channel) {
        log.info("接收构建任务: appId={}, workingDir={}", task.getAppId(), task.getWorkingDir());
        
        try {
            boolean buildSuccess = vueProjectBuilder.buildVueProject(task.getWorkingDir());
            
            if (buildSuccess) {
                log.info("构建成功: appId={}", task.getAppId());
            } else {
                VueProjectBuilder.BuildStatus status = vueProjectBuilder.getBuildStatus(task.getWorkingDir());
                String errorMsg = status != null && status.getErrorMessage() != null 
                        ? status.getErrorMessage() : "构建失败";
                log.error("构建失败: appId={}, error={}", task.getAppId(), errorMsg);
            }
            
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); // 手动确认消息
        } catch (Exception e) {
            log.error("处理构建任务异常: appId={}, error={}", task.getAppId(), e.getMessage(), e);
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // 手动拒绝消息
                throw e;
            } catch (Exception ex) {
                log.error("消息确认失败", ex);
            }
        }
    }
}