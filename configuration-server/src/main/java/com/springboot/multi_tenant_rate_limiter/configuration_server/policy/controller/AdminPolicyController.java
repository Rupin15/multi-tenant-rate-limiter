package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.controller;

import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.dto.UpdateRateLimitPolicyRequest;
import com.springboot.multi_tenant_rate_limiter.configuration_server.policy.services.RateLimitPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final RateLimitPolicyService policyService;

    @GetMapping
    public ResponseEntity<List<RateLimitPolicy>> getPolicies() {
        return ResponseEntity.ok(policyService.fetchAllPolicies());
    }

    @PostMapping
    public ResponseEntity<RateLimitPolicy> upsertPolicy(@Valid @RequestBody UpdateRateLimitPolicyRequest request) {
        return ResponseEntity.ok(policyService.upsertPolicy(request));
    }
}
