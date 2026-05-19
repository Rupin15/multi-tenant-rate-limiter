package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import java.util.concurrent.TimeUnit;

public enum TimeWindowUnit {

    SECOND {
        @Override
        public long toNanos(long value) {
            return TimeUnit.SECONDS.toNanos(value);
        }

        @Override
        public long fromNanos(long nanos) {
            return TimeUnit.NANOSECONDS.toSeconds(nanos);
        }

        @Override
        public long perUnitNano(){return TimeUnit.SECONDS.toNanos(1);}
    },

    MINUTE {
        @Override
        public long toNanos(long value) {
            return TimeUnit.MINUTES.toNanos(value);
        }

        @Override
        public long fromNanos(long nanos) {
            return TimeUnit.NANOSECONDS.toMinutes(nanos);
        }

        @Override
        public long perUnitNano(){return TimeUnit.MINUTES.toNanos(1);}
    },

    MILLISECOND {
        @Override
        public long toNanos(long value) {
            return TimeUnit.MILLISECONDS.toNanos(value);
        }

        @Override
        public long fromNanos(long nanos) {
            return TimeUnit.NANOSECONDS.toMillis(nanos);
        }

        @Override
        public long perUnitNano(){return TimeUnit.MILLISECONDS.toNanos(1);}
    },

    NANOSECONDS {
        @Override
        public long toNanos(long value) {
            return value;
        }

        @Override
        public long fromNanos(long nanos) {
            return nanos;
        }

        @Override
        public long perUnitNano(){return 1;}

    };

    public abstract long toNanos(long value);

    public abstract long fromNanos(long nanos);

    public abstract  long perUnitNano();
}
