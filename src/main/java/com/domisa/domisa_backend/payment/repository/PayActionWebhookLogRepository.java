package com.domisa.domisa_backend.payment.repository;

import com.domisa.domisa_backend.payment.entity.PayActionWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayActionWebhookLogRepository extends JpaRepository<PayActionWebhookLog, Long> {

	boolean existsByTraceId(String traceId);
}
