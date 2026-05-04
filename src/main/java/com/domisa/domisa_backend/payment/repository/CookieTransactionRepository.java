package com.domisa.domisa_backend.payment.repository;

import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CookieTransactionRepository extends JpaRepository<CookieTransaction, Long> {

	@Modifying
	@Query("""
		delete from CookieTransaction t
		where t.user.id = :userId
			or t.order.id in (
				select o.id from CookieOrder o where o.user.id = :userId
			)
		""")
	void deleteAllRelatedToUser(@Param("userId") Long userId);
}
