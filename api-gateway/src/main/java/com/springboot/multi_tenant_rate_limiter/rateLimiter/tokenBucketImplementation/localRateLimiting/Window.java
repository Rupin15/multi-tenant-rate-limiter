package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import org.springframework.stereotype.Component;

@Component
public class Window {

    private TimeWindowUnit unit;
    private double refillRatePerNanosecond;

    public Window(){}

    public Window(TimeWindowUnit unit, double value){
        this.unit = unit;
        this.refillRatePerNanosecond = value/unit.perUnitNano();
    }

    public double getToken(long difference, TimeWindowUnit timeWindowUnit) {
        long nanos = timeWindowUnit.toNanos(difference);
        return nanos * refillRatePerNanosecond;
    }

    public TimeWindowUnit getUnit() {
        return unit;
    }

    public double getRefillRatePerNanosecond() {
        return refillRatePerNanosecond;
    }
}
