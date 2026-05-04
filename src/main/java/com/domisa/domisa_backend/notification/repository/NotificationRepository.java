package com.domisa.domisa_backend.notification.repository;

import com.domisa.domisa_backend.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findAllByUserIdOrderByCreatedAtAsc(Long userId);

	List<Notification> findAllByUserIdAndIsCanceledFalseOrderByCreatedAtAsc(Long userId);

	long countByUserIdAndIsReadFalse(Long userId);

	Optional<Notification> findByIdAndUserId(Long notificationId, Long userId);

	void deleteByUserIdOrTargetUserId(Long userId, Long targetUserId);
}
