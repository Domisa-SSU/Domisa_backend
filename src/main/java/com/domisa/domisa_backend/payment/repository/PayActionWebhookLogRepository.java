package com.domisa.domisa_backend.payment.repository;

import com.domisa.domisa_backend.payment.entity.PayActionWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayActionWebhookLogRepository extends JpaRepository<PayActionWebhookLog, Long> {

	boolean existsByTraceId(String traceId);

	@Modifying
	@Query("""
		delete from PayActionWebhookLog l
		where l.orderNumber in (
			select o.orderNumber from CookieOrder o where o.user.id = :userId
		)
		""")
	void deleteByCookieOrderUserId(@Param("userId") Long userId);
}
