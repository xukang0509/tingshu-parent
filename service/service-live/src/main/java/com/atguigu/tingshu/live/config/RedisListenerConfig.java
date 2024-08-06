package com.atguigu.tingshu.live.config;

import com.atguigu.tingshu.live.util.WebSocketLocalContainer;
import com.atguigu.tingshu.model.live.SocketMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
public class RedisListenerConfig {
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * Redis 消息监听绑定监听指定通道
     * 可以添加多个监听器，监听多个通道，只需要将消息监听器与订阅的通道/主题绑定即可
     */
    @Bean
    public RedisMessageListenerContainer listenerContainer(
            RedisConnectionFactory connectionFactory, MessageListener messageListener) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        // 设置连接工厂，RedisConnectionFactory 可以直接从容器中取，也可以从 RedisTemplate 中取
        listenerContainer.setConnectionFactory(connectionFactory);
        // 可以同时订阅多个消息通道
        // 订阅名称叫 tingshu:live:message 的通道, 类似 Redis 中的 subscribe 命令
        // new PatternTopic("tingshu:*") 类似 Redis 的 pSubscribe 命令
        listenerContainer.addMessageListener(messageListener, new ChannelTopic("tingshu:live:message"));
        return listenerContainer;
    }

    /**
     * 初始化redis消息监听器
     */
    @Bean
    public MessageListener messageListener() {
        return (Message message, byte[] pattern) -> {
            // 消息来自于那个通道
            String channel = new String(pattern);
            log.info("消息通道: {}", channel);
            // System.out.println("消息通道: " + new String(message.getChannel()));
            // 消息内容
            SocketMsg msg = (SocketMsg) redisTemplate.getValueSerializer().deserialize(message.getBody());
            log.info("消息内容: {}", msg);
            // 发送消息
            WebSocketLocalContainer.sendMsg(msg);
        };
    }

}
