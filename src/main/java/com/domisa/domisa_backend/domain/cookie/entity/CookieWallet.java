package com.domisa.domisa_backend.domain.cookie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.domisa.domisa_backend.user.entity.User;

@Getter
@Entity
@Table(name = "cookie_wallets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CookieWallet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "balance", nullable = false)
	private Integer balance;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	private CookieWallet(User user, Integer balance) {
		this.user = user;
		this.balance = balance;
	}

	public static CookieWallet create(User user, Integer balance) {
		return new CookieWallet(user, balance);
	}

	public void add(Integer amount) {
		this.balance += amount;
	}

	public void subtract(Integer amount) {
		this.balance -= amount;
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
