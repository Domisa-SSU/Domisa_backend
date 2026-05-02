package com.domisa.domisa_backend.payment.repository;

import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CookieOrderRepository extends JpaRepository<CookieOrder, Long> {

	Optional<CookieOrder> findByOrderNumber(String orderNumber);

	Optional<CookieOrder> findByUserIdAndBillingNameAndOrderAmount(Long userId, String billingName, Integer orderAmount);

	boolean existsByBillingNameAndStatus(String billingName, OrderStatus status);

	Optional<CookieOrder> findTopByOrderNumberStartingWithOrderByOrderNumberDesc(String prefix);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from CookieOrder o where o.orderNumber = :orderNumber")
	Optional<CookieOrder> findByOrderNumberWithLock(@Param("orderNumber") String orderNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select o from CookieOrder o
		where o.user.id = :userId
			and o.billingName = :billingName
			and o.orderAmount = :orderAmount
		""")
	Optional<CookieOrder> findByUserIdAndBillingNameAndOrderAmountWithLock(
		@Param("userId") Long userId,
		@Param("billingName") String billingName,
		@Param("orderAmount") Integer orderAmount
	);
}
