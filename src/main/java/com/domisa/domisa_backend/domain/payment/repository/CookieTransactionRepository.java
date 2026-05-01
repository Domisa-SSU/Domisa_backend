package com.domisa.domisa_backend.domain.payment.repository;

import com.domisa.domisa_backend.domain.payment.entity.CookieTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookieTransactionRepository extends JpaRepository<CookieTransaction, Long> {
}
