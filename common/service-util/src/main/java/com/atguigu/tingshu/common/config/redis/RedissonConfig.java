package com.atguigu.tingshu.common.config.redis;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * redisson配置信息
 *
 * @author xk
 */
@Data
@Configuration
@ConfigurationProperties("spring.data.redis")
public class RedissonConfig {
    private String host;
    private String password;
    private String port;
    private int timeout = 3000;
    private static final String ADDRESS_PREFIX = "redis://";

    /**
     * 自动装配
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        if (!StringUtils.hasText(host)) {
            throw new RuntimeException("host is empty");
        }
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(ADDRESS_PREFIX + this.host + ":" + this.port)
                .setTimeout(this.timeout);
        if (StringUtils.hasText(this.password)) {
            serverConfig.setPassword(this.password);
        }
        return Redisson.create(config);
    }
}