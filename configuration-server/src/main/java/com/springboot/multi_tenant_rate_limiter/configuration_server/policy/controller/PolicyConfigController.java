package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.controller;

import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.services.RateLimitPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/config/rate-limit-policies")
@RequiredArgsConstructor
public class PolicyConfigController {

    private final RateLimitPolicyService policyService;

    @GetMapping
    public ResponseEntity<List<RateLimitPolicy>> getAllPolicies() {
        return ResponseEntity.ok(policyService.fetchAllPolicies());
    }
}
