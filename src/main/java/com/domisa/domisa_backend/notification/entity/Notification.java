package com.domisa.domisa_backend.notification.entity;

import com.domisa.domisa_backend.notification.type.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "target_user_id")
	private Long targetUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private NotificationType type;

	@Column(name = "is_read", nullable = false)
	private boolean isRead = false;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	private Notification(Long userId, NotificationType type) {
		this.userId = userId;
		this.type = type;
		this.isRead = false;
		this.createdAt = LocalDateTime.now();
	}

	private Notification(Long userId, Long targetUserId, NotificationType type) {
		this.userId = userId;
		this.targetUserId = targetUserId;
		this.type = type;
		this.isRead = false;
		this.createdAt = LocalDateTime.now();
	}

	public static Notification create(Long userId, NotificationType type) {
		return new Notification(userId, type);
	}

	public static Notification create(Long userId, Long targetUserId, NotificationType type) {
		return new Notification(userId, targetUserId, type);
	}

	public void markAsRead() {
		this.isRead = true;
	}
}
