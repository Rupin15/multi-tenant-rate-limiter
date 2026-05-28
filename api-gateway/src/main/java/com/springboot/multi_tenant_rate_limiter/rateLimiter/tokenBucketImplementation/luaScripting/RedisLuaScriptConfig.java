package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.luaScripting;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisLuaScriptConfig {

    @Bean
    public RedisScript<Long> rateLimiterScript() {
        return RedisScript.of(new ClassPathResource("script/tokenBucket.lua"), Long.class);
    }

    @Bean
    public RedisScript<Long> leaseScript() {
        return RedisScript.of(new ClassPathResource("script/refillLocal.lua"), Long.class);
    }
}
