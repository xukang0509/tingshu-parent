package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * 定时解锁余额 延时队列的配置
 *
 * @author xk
 * @since 2024-08-17 16:44
 */
@Configuration
public class AccountUnlockDelayQueueConfig {
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(RabbitMqConstant.EXCHANGE_ACCOUNT, true, false, null);
    }

    @Bean
    public Queue queue() {
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", RabbitMqConstant.EXCHANGE_ACCOUNT_UNLOCK);
        arguments.put("x-dead-letter-routing-key", RabbitMqConstant.ROUTING_ACCOUNT_UNLOCK);
        arguments.put("x-message-ttl", 90 * 1000);
        return new Queue(RabbitMqConstant.QUEUE_ACCOUNT_UNLOCK_DEAD, true, false, false, arguments);
    }

    @Bean
    public Binding binding(DirectExchange directExchange, Queue queue) {
        return BindingBuilder.bind(queue).to(directExchange).with(RabbitMqConstant.ROUTING_ACCOUNT_UNLOCK_DEAD);
    }
}
