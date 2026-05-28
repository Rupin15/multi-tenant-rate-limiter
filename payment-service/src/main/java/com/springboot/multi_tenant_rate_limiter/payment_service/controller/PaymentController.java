package com.springboot.multi_tenant_rate_limiter.payment_service.controller;

import io.micrometer.observation.annotation.Observed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    @Observed(name = "payment.healthcheck")
    @GetMapping("/v1/payments/healthcheck")
    public String healtcheck(){
        return "Payment Working";
    }
}
