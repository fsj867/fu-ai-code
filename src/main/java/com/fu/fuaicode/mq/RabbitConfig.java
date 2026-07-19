package com.fu.fuaicode.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory); // 创建RabbitTemplate实例
        rabbitTemplate.setMessageConverter(messageConverter);   // 设置消息转换器
        // 消息持久化
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
        // 消息确认回调
        // 当消息被确认或拒绝时，会触发该回调
        // 回调参数：correlationData, ack, cause
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息确认成功: correlationId={}", correlationData != null ? correlationData.getId() : "null");
            } else {
                log.error("消息确认失败: correlationId={}, cause={}", 
                        correlationData != null ? correlationData.getId() : "null", cause);
            }
        });
        // 消息返回回调
        // 当消息没有被路由到队列时，会触发该回调
        // 回调参数：message, replyCode, replyText, exchange, routingKey
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            Message message = returnedMessage.getMessage();
            int replyCode = returnedMessage.getReplyCode();
            String replyText = returnedMessage.getReplyText();
            String exchange = returnedMessage.getExchange();
            String routingKey = returnedMessage.getRoutingKey();
            log.error("消息返回失败: exchange={}, routingKey={}, replyCode={}, replyText={}", 
                    exchange, routingKey, replyCode, replyText);
        });

        return rabbitTemplate;
    }
    // 配置交换机
    @Bean
    public DirectExchange exchange(){
        return ExchangeBuilder.directExchange(MqConstant.EXCHANGE_NAME)
        .durable(true)
        .build();
    }
    // 配置队列
    @Bean
    public Queue queue(){
        return QueueBuilder.durable(MqConstant.QUEUE_NAME).build();
    }
    // 配置绑定
    // 将队列绑定到交换机，指定路由键
    // 交换机：将消息路由到队列
    // 路由键：用于将消息路由到队列的规则
    @Bean
    public Binding binding(Queue queue, DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(MqConstant.ROUTING_KEY);
    }
}
