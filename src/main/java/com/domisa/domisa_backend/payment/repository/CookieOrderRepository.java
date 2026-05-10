package com.domisa.domisa_backend.payment.repository;

import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CookieOrderRepository extends JpaRepository<CookieOrder, Long> {

	boolean existsByBillingNameAndStatus(String billingName, OrderStatus status);

	void deleteByUserId(Long userId);

	@Query("""
		select o
		from CookieOrder o
		join fetch o.user
		order by o.createdAt desc, o.id desc
		""")
	List<CookieOrder> findAllForDms();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<CookieOrder> findByUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from CookieOrder o where o.orderNumber = :orderNumber")
	Optional<CookieOrder> findByOrderNumberWithLock(@Param("orderNumber") String orderNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select o from CookieOrder o
		where o.user.id = :userId
			and o.billingName = :billingName
			and o.orderAmount = :orderAmount
		order by o.createdAt desc
		""")
	List<CookieOrder> findAllByUserIdAndBillingNameAndOrderAmountWithLock(
		@Param("userId") Long userId,
		@Param("billingName") String billingName,
		@Param("orderAmount") Integer orderAmount
	);
}
