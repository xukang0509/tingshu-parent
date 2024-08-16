package com.atguigu.tingshu.user.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
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
 * @since 2024-08-16 16:52
 */
@Component
@Slf4j
public class UserPaidRecordReceiver {
    @Resource
    private UserInfoService userInfoService;


    /**
     * 更新用户购买记录
     *
     * @param userPaidRecordVoStr
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = RabbitMqConstant.EXCHANGE_USER_PAY_RECORD, durable = "true"),
                    value = @Queue(value = RabbitMqConstant.QUEUE_USER_PAY_RECORD, durable = "true"),
                    key = RabbitMqConstant.ROUTING_USER_PAY_RECORD
            )
    )
    public void updateUserPayRecord(String userPaidRecordVoStr, Message message, Channel channel) {
        if (StringUtils.isBlank(userPaidRecordVoStr)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        try {
            UserPaidRecordVo userPaidRecordVo = JSON.parseObject(userPaidRecordVoStr, UserPaidRecordVo.class);
            this.userInfoService.updateUserPayRecord(userPaidRecordVo);
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
