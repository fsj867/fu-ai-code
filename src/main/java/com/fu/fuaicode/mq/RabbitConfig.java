package com.fu.fuaicode.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitConfig {
    // 配置交换机
    @Bean
    public DirectExchange exchange(){
        return ExchangeBuilder.directExchange(MqConstant.EXCHANGE_NAME) // 直连交换机
        .durable(true) // 持久化交换机
        .build();
    }
    // 配置队列
    @Bean
    public Queue queue(){
        return QueueBuilder.durable(MqConstant.QUEUE_NAME).build();// 持久化队列
    }
    // 配置绑定
    @Bean
    public Binding binding(Queue queue, DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(MqConstant.ROUTING_KEY);
    }
}
