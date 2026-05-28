DROP TABLE IF EXISTS policy_event_outbox;
DROP TABLE IF EXISTS rate_limit_policies;

CREATE TABLE rate_limit_policies (route_id VARCHAR(255) NOT NULL,
                                     tier VARCHAR(32) NOT NULL,
                                     name VARCHAR(255) NOT NULL,
                                     max_tokens BIGINT NOT NULL CHECK (max_tokens > 0),
                                     refill_tokens_per_second BIGINT NOT NULL CHECK (refill_tokens_per_second > 0),
                                     lease_size BIGINT NOT NULL CHECK (lease_size > 0),
                                     version BIGINT NOT NULL DEFAULT 1,
                                     entity_version BIGINT NOT NULL DEFAULT 0,
                                     last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                     PRIMARY KEY (route_id, tier)
);


CREATE INDEX idx_rate_limit_policy_route  ON rate_limit_policies(route_id);

CREATE INDEX idx_rate_limit_policy_last_updated ON rate_limit_policies(last_updated);


INSERT INTO rate_limit_policies (
    route_id,
    tier,
    name,
    max_tokens,
    refill_tokens_per_second,
    lease_size
)
VALUES
    (
        'payment-gateway', 'FREE', 'PAYMENTS_FREE', 30, 2,  2
    ),

    (
        'payment-gateway', 'PRO',  'PAYMENTS_PRO',  80,  5, 5
    ),

    (
        'payment-gateway','ENTERPRISE','PAYMENTS_ENTERPRISE', 200,20,15
    ),

    (
        'order-gateway', 'FREE', 'ORDERS_FREE',   40,  3, 3
    ),

    (
        'order-gateway',  'PRO','ORDERS_PRO',120,8,8
    ),

    (
        'order-gateway','ENTERPRISE','ORDERS_ENTERPRISE',300,30, 20
    ),
    (
        'default',  'FREE',  'DEFAULT_FREE',  20,  2,1
    ),
    (
        'default','PRO','DEFAULT_PRO', 80, 8,5
    ),

    (
        'default','ENTERPRISE','DEFAULT_ENTERPRISE', 200,20,10
    );

CREATE TABLE policy_event_outbox (   id VARCHAR(255) PRIMARY KEY,
                                     route_id VARCHAR(255) NOT NULL,
                                     tier VARCHAR(32) NOT NULL,
                                     payload TEXT NOT NULL,
                                     processed BOOLEAN NOT NULL DEFAULT FALSE,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW());

CREATE INDEX idx_policy_outbox_processed_created    ON policy_event_outbox(processed, created_at);

CREATE INDEX idx_policy_outbox_route_tier  ON policy_event_outbox(route_id, tier);

CREATE INDEX idx_policy_outbox_created  ON policy_event_outbox(created_at);