package com.springboot.multi_tenant_rate_limiter.controller;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthCheck {

    @Observed(name="api.healthcheck")
    @GetMapping("/healthcheck")
    public String healthcheck() throws InterruptedException {
        return "UP";
    }
}
