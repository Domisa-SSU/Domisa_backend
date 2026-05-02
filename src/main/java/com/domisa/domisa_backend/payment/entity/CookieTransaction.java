package com.domisa.domisa_backend.payment.entity;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cookie_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CookieTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private CookieOrder order;

	@Column(name = "amount", nullable = false)
	private Integer amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private CookieTransactionType type;

	@Column(name = "description", length = 100)
	private String description;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	private CookieTransaction(
		User user,
		CookieOrder order,
		Integer amount,
		CookieTransactionType type,
		String description
	) {
		this.user = user;
		this.order = order;
		this.amount = amount;
		this.type = type;
		this.description = description;
	}

	public static CookieTransaction charge(User user, CookieOrder order, Integer amount) {
		return new CookieTransaction(user, order, amount, CookieTransactionType.CHARGE, "쿠키 충전");
	}

	public static CookieTransaction use(User user, Integer amount, String description) {
		return new CookieTransaction(user, null, amount, CookieTransactionType.USE, description);
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
}
