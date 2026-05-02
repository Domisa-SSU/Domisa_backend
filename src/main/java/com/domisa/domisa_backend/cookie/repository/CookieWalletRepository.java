package com.domisa.domisa_backend.cookie.repository;

import com.domisa.domisa_backend.cookie.entity.CookieWallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CookieWalletRepository extends JpaRepository<CookieWallet, Long> {

	Optional<CookieWallet> findByUserId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from CookieWallet w where w.user.id = :userId")
	Optional<CookieWallet> findByUserIdWithLock(@Param("userId") Long userId);
}
