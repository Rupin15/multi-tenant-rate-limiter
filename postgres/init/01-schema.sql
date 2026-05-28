DROP TABLE IF EXISTS rate_limit_policies;

CREATE TABLE rate_limit_policies (
                                     route_id VARCHAR(255) PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
                                     max_tokens BIGINT NOT NULL,
                                     refill_tokens_per_second BIGINT NOT NULL,
                                     lease_size BIGINT NOT NULL,
                                     version BIGINT NOT NULL DEFAULT 0,
                                     entity_version BIGINT NOT NULL DEFAULT 0,
                                     last_updated TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO rate_limit_policies (
    route_id,
    name,
    max_tokens,
    refill_tokens_per_second,
    lease_size
)
VALUES
    (
        'payment-gateway',
        'PAYMENTS',
        50,
        2,
        3
    ),
    (
        'order-gateway',
        'ORDERS',
        100,
        5,
        10
    );

CREATE TABLE policy_event_outbox (
                                     id VARCHAR(255) PRIMARY KEY,
                                     policy_name VARCHAR(255) NOT NULL,
                                     payload TEXT NOT NULL,
                                     processed BOOLEAN NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_policy_outbox_processed_created
    ON policy_event_outbox(processed, created_at);