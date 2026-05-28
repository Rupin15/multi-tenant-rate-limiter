package com.springboot.multi_tenant_rate_limiter.chaos;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChaosInjectionService {

    private final AtomicBoolean redisLeaseFailureEnabled = new AtomicBoolean(false);
    private final AtomicBoolean configSyncFailureEnabled = new AtomicBoolean(false);

    public boolean isRedisLeaseFailureEnabled() {
        return redisLeaseFailureEnabled.get();
    }

    public boolean isConfigSyncFailureEnabled() {
        return configSyncFailureEnabled.get();
    }

    public ChaosState updateState(boolean redisLeaseFailureEnabled, boolean configSyncFailureEnabled) {
        this.redisLeaseFailureEnabled.set(redisLeaseFailureEnabled);
        this.configSyncFailureEnabled.set(configSyncFailureEnabled);
        return currentState();
    }

    public ChaosState currentState() {
        return new ChaosState(redisLeaseFailureEnabled.get(), configSyncFailureEnabled.get());
    }

    public record ChaosState(boolean redisLeaseFailureEnabled, boolean configSyncFailureEnabled) {
    }
}
