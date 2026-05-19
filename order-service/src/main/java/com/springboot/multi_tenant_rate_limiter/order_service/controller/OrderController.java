package com.springboot.multi_tenant_rate_limiter.order_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping("/v1/orders/healthcheck")
    public String healtcheck(){
        return "Orders Working";
    }
}
