package com.fu.fuaicode.ratelimit.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    // Redis 配置
    @Value("${spring.data.redis.host}")
    private String redisHost;   // Redis 主机地址

    @Value("${spring.data.redis.port}")
    private Integer redisPort;   // Redis 端口号

    @Value("${spring.data.redis.password}")
    private String redisPassword;   // Redis 密码

    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;   // Redis 数据库索引

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setLockWatchdogTimeout(10000L); // 设置锁看门超时时间，默认10秒
        // address
        String address = "redis://" + redisHost + ":" + redisPort;
        // 设置单机模式
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address) // 设置 Redis 地址
                .setDatabase(redisDatabase) // 设置数据库索引
                .setConnectionMinimumIdleSize(1) // 最小空闲连接数，默认1个
                .setConnectionPoolSize(10) // 连接池大小，默认10个
                .setIdleConnectionTimeout(30000) // 空闲连接超时时间，默认30秒
                .setConnectTimeout(5000) // 连接超时时间，默认5秒
                .setTimeout(3000) // 超时时间，默认3秒
                .setRetryAttempts(3)  // 重试次数，默认3次
                .setRetryInterval(1500);     // 重试间隔时间
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword); // 设置 Redis 密码
        }
        return Redisson.create(config);
    }
}
