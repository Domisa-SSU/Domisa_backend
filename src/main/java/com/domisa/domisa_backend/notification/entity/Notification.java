package com.domisa.domisa_backend.notification.entity;

import com.domisa.domisa_backend.notification.type.NotificationType;
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

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

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

	private Notification(User user, NotificationType type, String title, String content) {
		this.user = user;
		this.type = type;
		this.title = title;
		this.content = content;
		this.isRead = false;
		this.createdAt = LocalDateTime.now();
	}

	public static Notification create(User user, NotificationType type, String title, String content) {
		return new Notification(user, type, title, content);
	}

	public void markAsRead() {
		this.isRead = true;
	}
}
