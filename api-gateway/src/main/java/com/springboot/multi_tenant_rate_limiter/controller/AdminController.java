package com.springboot.multi_tenant_rate_limiter.controller;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.RateLimitPolicyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final RateLimitPolicyResolver resolver;

    @PostMapping("/update-policy")
    public ResponseEntity<String> updatePolicy(@RequestParam String policyName, @RequestBody RateLimitPolicy policy) {
        log.info("inside the controller");
        return ResponseEntity.ok(resolver.updatePolicy(policyName, policy));
    }
}