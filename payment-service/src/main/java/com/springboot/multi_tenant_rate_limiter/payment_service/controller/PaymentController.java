package com.springboot.multi_tenant_rate_limiter.payment_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    @GetMapping("/v1/payments/healthcheck")
    public String healtcheck(){
        return "Payment Working";
    }
}
