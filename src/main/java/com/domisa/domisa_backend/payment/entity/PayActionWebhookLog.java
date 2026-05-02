package com.domisa.domisa_backend.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payaction_webhook_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayActionWebhookLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "trace_id", nullable = false, unique = true)
	private String traceId;

	@Column(name = "order_number", nullable = false)
	private String orderNumber;

	@Column(name = "order_status", nullable = false)
	private String orderStatus;

	@Column(name = "processing_date", nullable = false)
	private LocalDateTime processingDate;

	@Column(name = "received_at", nullable = false)
	private LocalDateTime receivedAt;

	private PayActionWebhookLog(
		String traceId,
		String orderNumber,
		String orderStatus,
		LocalDateTime processingDate,
		LocalDateTime receivedAt
	) {
		this.traceId = traceId;
		this.orderNumber = orderNumber;
		this.orderStatus = orderStatus;
		this.processingDate = processingDate;
		this.receivedAt = receivedAt;
	}

	public static PayActionWebhookLog create(
		String traceId,
		String orderNumber,
		String orderStatus,
		LocalDateTime processingDate,
		LocalDateTime receivedAt
	) {
		return new PayActionWebhookLog(traceId, orderNumber, orderStatus, processingDate, receivedAt);
	}
}
