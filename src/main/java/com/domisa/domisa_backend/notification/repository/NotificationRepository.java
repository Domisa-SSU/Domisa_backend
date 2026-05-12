package com.domisa.domisa_backend.notification.repository;

import com.domisa.domisa_backend.notification.entity.Notification;
import com.domisa.domisa_backend.notification.type.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findAllByUserIdOrderByCreatedAtAsc(Long userId);

	List<Notification> findAllByUserIdAndIsCanceledFalseOrderByCreatedAtAsc(Long userId);

	long countByUserIdAndIsReadFalse(Long userId);

	Optional<Notification> findByIdAndUserId(Long notificationId, Long userId);

	void deleteByUserIdOrTargetUserId(Long userId, Long targetUserId);

	@Query("""
		select n
		from Notification n
		join User u on u.id = n.userId
		where n.isRead = false
			and n.type in :types
			and u.notificationPhone is not null
			and u.notificationPhone <> ''
		order by n.userId asc, n.createdAt asc
		""")
	List<Notification> findUnreadSmsTargetNotifications(@Param("types") List<NotificationType> types);
}
