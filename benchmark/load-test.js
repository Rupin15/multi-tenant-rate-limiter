import http from 'k6/http';
import { check } from 'k6';

const RATE = __ENV.RATE ? parseInt(__ENV.RATE) : 500;

export const options = {
    scenarios: {
        gateway_load_test: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: Math.max(100, RATE / 5),
            maxVUs: Math.max(1000, RATE * 2)
        }
    },

    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500']
    }
};

export default function () {
    const response = http.get(
        'http://localhost:8084/api/payments/healthcheck',
        {
            headers: {
                'X-Tenant-Tier': 'FREE'
            }
        }
    );

    check(response, {
        'status is valid': (r) =>
            r.status === 200 || r.status === 429
    });
}