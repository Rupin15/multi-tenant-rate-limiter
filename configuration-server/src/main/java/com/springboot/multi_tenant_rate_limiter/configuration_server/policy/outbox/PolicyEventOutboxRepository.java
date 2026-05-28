package com.springboot.multi_tenant_rate_limiter.configuration_server.policy.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyEventOutboxRepository extends JpaRepository<PolicyEventOutboxEntity, String> {
    List<PolicyEventOutboxEntity> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
