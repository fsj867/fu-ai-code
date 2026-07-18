package com.fu.fuaicode.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class BuildTaskProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendBuildTask(BuildTask task) {
        String messageId = UUID.randomUUID().toString();
        log.info("发送构建任务: messageId={}, appId={}, workingDir={}", 
                messageId, task.getAppId(), task.getWorkingDir());
        if (task.getAppId() == null) {
            log.error("构建任务的appId不能为空");
            return;
        }
        if (task.getWorkingDir() == null) {
            log.error("构建任务的workingDir不能为空");
            return;
        }
        if (task.getGeneratedCodeDir() == null) {
            log.error("构建任务的generatedCodeDir不能为空");
            return;
               }

        CorrelationData correlationData = new CorrelationData(messageId); // 关联数据，用于跟踪消息
        // 发送消息到队列
        rabbitTemplate.convertAndSend(
                MqConstant.EXCHANGE_NAME,
                MqConstant.ROUTING_KEY,
                task,
                correlationData
        );
    }
}