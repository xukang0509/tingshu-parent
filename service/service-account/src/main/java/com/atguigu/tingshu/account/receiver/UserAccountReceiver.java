package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.rabbitmq.client.Channel;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author xk
 * @since 2024-08-16 14:10
 */
@Component
@Slf4j
public class UserAccountReceiver {
    @Resource
    private UserAccountService userAccountService;

    /**
     * 账户余额支付成功：发送消息扣键余额
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = RabbitMqConstant.EXCHANGE_ACCOUNT_MINUS, durable = "true"),
                    value = @Queue(value = RabbitMqConstant.QUEUE_ACCOUNT_MINUS, durable = "true"),
                    key = RabbitMqConstant.ROUTING_ACCOUNT_MINUS
            )
    )
    public void minus(String orderNo, Message message, Channel channel) {
        if (StringUtils.isBlank(orderNo)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        try {
            // 扣键余额
            this.userAccountService.minus(orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            if (!message.getMessageProperties().getRedelivered()) {
                // 消息已重复处理,拒绝再次接收
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 该消息不是重复的
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }


    /**
     * 账户余额支付失败：发送消息解锁余额
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = RabbitMqConstant.EXCHANGE_ACCOUNT_UNLOCK, durable = "true"),
                    value = @Queue(value = RabbitMqConstant.QUEUE_ACCOUNT_UNLOCK, durable = "true"),
                    key = RabbitMqConstant.ROUTING_ACCOUNT_UNLOCK
            )
    )
    public void unlock(String orderNo, Message message, Channel channel) {
        if (StringUtils.isBlank(orderNo)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        try {
            // 解锁余额
            this.userAccountService.unlock(orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            if (!message.getMessageProperties().getRedelivered()) {
                // 消息已重复处理,拒绝再次接收
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 该消息不是重复的
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
