package com.domisa.domisa_backend.domain.payment.repository;

import com.domisa.domisa_backend.domain.payment.entity.CookieOrder;
import com.domisa.domisa_backend.domain.payment.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CookieOrderRepository extends JpaRepository<CookieOrder, Long> {

	Optional<CookieOrder> findByOrderNumber(String orderNumber);

	boolean existsByBillingNameAndOrderAmountAndStatus(String billingName, Integer orderAmount, OrderStatus status);

	Optional<CookieOrder> findTopByOrderNumberStartingWithOrderByOrderNumberDesc(String prefix);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from CookieOrder o where o.orderNumber = :orderNumber")
	Optional<CookieOrder> findByOrderNumberWithLock(@Param("orderNumber") String orderNumber);
}
