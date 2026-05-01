package com.domisa.domisa_backend.domain.payment.entity;

import com.domisa.domisa_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cookie_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CookieOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "order_number", length = 22, nullable = false, unique = true)
	private String orderNumber;

	@Column(name = "billing_name", nullable = false)
	private String billingName;

	@Column(name = "order_amount", nullable = false)
	private Integer orderAmount;

	@Column(name = "cookie_amount", nullable = false)
	private Integer cookieAmount;

	@Column(name = "orderer_name", nullable = false)
	private String ordererName;

	@Column(name = "order_date", nullable = false)
	private LocalDateTime orderDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "payaction_processing_date")
	private LocalDateTime payactionProcessingDate;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	private CookieOrder(
		User user,
		String orderNumber,
		String billingName,
		Integer orderAmount,
		Integer cookieAmount,
		String ordererName,
		LocalDateTime orderDate
	) {
		this.user = user;
		this.orderNumber = orderNumber;
		this.billingName = billingName;
		this.orderAmount = orderAmount;
		this.cookieAmount = cookieAmount;
		this.ordererName = ordererName;
		this.orderDate = orderDate;
		this.status = OrderStatus.PENDING;
	}

	public static CookieOrder create(
		User user,
		String orderNumber,
		String billingName,
		Integer orderAmount,
		Integer cookieAmount,
		String ordererName,
		LocalDateTime orderDate
	) {
		return new CookieOrder(user, orderNumber, billingName, orderAmount, cookieAmount, ordererName, orderDate);
	}

	public boolean isPaid() {
		return status == OrderStatus.PAID;
	}

	public boolean isPending() {
		return status == OrderStatus.PENDING;
	}

	public void markPaid(LocalDateTime processingDate) {
		this.status = OrderStatus.PAID;
		this.payactionProcessingDate = processingDate;
		this.paidAt = processingDate == null ? LocalDateTime.now() : processingDate;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
