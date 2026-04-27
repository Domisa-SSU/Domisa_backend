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

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "is_read", nullable = false)
	private boolean isRead = false;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	private Notification(Long userId, Long targetUserId, NotificationType type, String title, String content) {
		this.userId = userId;
		this.targetUserId = targetUserId;
		this.type = type;
		this.title = title;
		this.content = content;
		this.isRead = false;
		this.createdAt = LocalDateTime.now();
	}

	public static Notification create(
		Long userId,
		Long targetUserId,
		NotificationType type,
		String title,
		String content
	) {
		return new Notification(userId, targetUserId, type, title, content);
	}

	public static Notification create(Long userId, Long targetUserId, NotificationTemplate template) {
		return new Notification(userId, targetUserId, template.type(), template.title(), template.content());
	}

	public void markAsRead() {
		this.isRead = true;
	}

	public record NotificationTemplate(
		NotificationType type,
		String title,
		String content
	) {
	}
}
