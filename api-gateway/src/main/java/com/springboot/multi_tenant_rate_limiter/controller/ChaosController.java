package com.springboot.multi_tenant_rate_limiter.controller;

import com.springboot.multi_tenant_rate_limiter.chaos.ChaosInjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/chaos")
@RequiredArgsConstructor
public class ChaosController {

    private final ChaosInjectionService chaosInjectionService;

    @GetMapping
    public ResponseEntity<ChaosInjectionService.ChaosState> getChaosState() {
        return ResponseEntity.ok(chaosInjectionService.currentState());
    }

    @PostMapping
    public ResponseEntity<ChaosInjectionService.ChaosState> updateChaosState(@RequestBody ChaosRequest request) {
        return ResponseEntity.ok(
                chaosInjectionService.updateState(
                        request.redisLeaseFailureEnabled(),
                        request.configSyncFailureEnabled()
                )
        );
    }

    public record ChaosRequest(boolean redisLeaseFailureEnabled, boolean configSyncFailureEnabled) {
    }
}
