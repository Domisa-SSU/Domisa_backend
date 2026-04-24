package com.domisa.domisa_backend.user.repository;

import com.domisa.domisa_backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(String kakaoId);
    boolean existsByNickname(String nickname);
}
