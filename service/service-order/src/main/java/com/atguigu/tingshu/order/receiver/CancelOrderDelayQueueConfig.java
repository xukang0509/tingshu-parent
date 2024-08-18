package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * 定时取消订单 延时队列的配置
 *
 * @author xk
 * @since 2024-08-17 15:49
 */
@Configuration
public class CancelOrderDelayQueueConfig {

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(RabbitMqConstant.EXCHANGE_ORDER, true, false, null);
    }

    @Bean
    public Queue queue1() {
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", RabbitMqConstant.EXCHANGE_ORDER);
        arguments.put("x-dead-letter-routing-key", RabbitMqConstant.ROUTING_CANCEL_ORDER);
        arguments.put("x-message-ttl", 90 * 1000);
        return new Queue(RabbitMqConstant.QUEUE_ORDER_DEAD, true, false, false, arguments);
    }

    @Bean
    public Binding binding(DirectExchange directExchange, Queue queue1) {
        return BindingBuilder.bind(queue1).to(directExchange).with(RabbitMqConstant.ROUTING_ORDER_DEAD);
    }

    @Bean
    public Queue queue2() {
        return new Queue(RabbitMqConstant.QUEUE_CANCEL_ORDER, true, false, false);
    }

    @Bean
    public Binding binding2(DirectExchange directExchange, Queue queue2) {
        return BindingBuilder.bind(queue2).to(directExchange).with(RabbitMqConstant.ROUTING_CANCEL_ORDER);
    }
}
