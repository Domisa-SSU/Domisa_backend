package com.domisa.domisa_backend.payment.repository;

import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookieTransactionRepository extends JpaRepository<CookieTransaction, Long> {
}
