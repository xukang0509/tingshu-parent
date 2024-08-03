package com.atguigu.tingshu.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.entity.GuiguCorrelationData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RabbitInitConfigApplicationListener implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        this.setupCallbacks();
    }

    private void setupCallbacks() {
        /**
         * 只确认消息是否正确到达 Exchange 中,成功与否都会回调
         *
         * @param correlation 相关数据  非消息本身业务数据
         * @param ack             应答结果
         * @param reason           如果发送消息到交换器失败，错误原因
         */
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, reason) -> {
            if (ack) {
                //消息到交换器成功
                log.info("消息发送到Exchange成功：{}", correlationData);
            } else {
                //消息到交换器失败
                log.error("消息发送到Exchange失败：data：{}，reason：{}", correlationData, reason);
                //执行消息重发
                this.retrySendMsg(correlationData);
            }
        });

        /**
         * 消息没有正确到达队列时触发回调，如果正确到达队列不执行
         */
        this.rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Returned: " + returned.getMessage() + "\nreplyCode: " + returned.getReplyCode()
                    + "\nreplyText: " + returned.getReplyText() + "\nexchange/rk: "
                    + returned.getExchange() + "/" + returned.getRoutingKey());
            //当路由队列失败 也需要重发
            //1.构建相关数据对象
            String redisKey = returned.getMessage().getMessageProperties().getHeader("spring_returned_message_correlation");
            String correlationDataStr = (String) redisTemplate.opsForValue().get(redisKey);
            GuiguCorrelationData guiguCorrelationData = JSON.parseObject(correlationDataStr, GuiguCorrelationData.class);
            //2.调用消息重发方法
            this.retrySendMsg(guiguCorrelationData);
        });
    }

    /**
     * 消息重新发送
     *
     * @param correlationData
     */
    private void retrySendMsg(CorrelationData correlationData) {
        //获取相关数据
        GuiguCorrelationData gmallCorrelationData = (GuiguCorrelationData) correlationData;

        //获取redis中存放重试次数
        //先重发，在写会到redis中次数
        int retryCount = gmallCorrelationData.getRetryCount();
        if (retryCount >= 3) {
            //超过最大重试次数
            log.error("生产者超过最大重试次数，将失败的消息存入数据库用人工处理；给管理员发送邮件；给管理员发送短信；");
            return;
        }
        //重发消息
        rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),
                gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), gmallCorrelationData);
        //重发次数+1
        retryCount += 1;
        gmallCorrelationData.setRetryCount(retryCount);
        redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(gmallCorrelationData), 10, TimeUnit.MINUTES);
        log.info("进行消息重发！");
    }
}
