package com.springboot.multi_tenant_rate_limiter.order_service.controller;

import io.micrometer.observation.annotation.Observed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @Observed(name = "order.healthcheck")
    @GetMapping("/v1/orders/healthcheck")
    public String healtcheck(){
        return "Orders Working";
    }
}
