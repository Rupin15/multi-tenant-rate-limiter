package com.springboot.multi_tenant_rate_limiter.chaos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChaosInjectionServiceTest {

    @Test
    void shouldUpdateAndExposeCurrentChaosState() {
        ChaosInjectionService chaosInjectionService = new ChaosInjectionService();

        ChaosInjectionService.ChaosState updated = chaosInjectionService.updateState(true, true);

        assertThat(updated.redisLeaseFailureEnabled()).isTrue();
        assertThat(updated.configSyncFailureEnabled()).isTrue();
        assertThat(chaosInjectionService.currentState().redisLeaseFailureEnabled()).isTrue();
        assertThat(chaosInjectionService.currentState().configSyncFailureEnabled()).isTrue();
    }
}
