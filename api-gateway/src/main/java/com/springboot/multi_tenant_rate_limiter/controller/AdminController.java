package com.springboot.multi_tenant_rate_limiter.controller;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.RateLimitPolicy;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.RateLimitPolicyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RateLimitPolicyResolver rateLimitPolicyResolver;

    @PostMapping("/update-policy")
    public ResponseEntity<String> updatePolicy(@RequestParam String policyName,@RequestBody RateLimitPolicy rateLimitPolicy) {
        rateLimitPolicyResolver.updatePolicy(policyName, rateLimitPolicy);
        return ResponseEntity.ok(
                "Policy updated successfully for api: " + policyName
        );
    }

}