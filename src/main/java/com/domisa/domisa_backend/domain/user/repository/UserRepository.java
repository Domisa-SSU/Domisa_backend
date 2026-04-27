package com.domisa.domisa_backend.domain.user.repository;

import com.domisa.domisa_backend.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKakaoId(Long kakaoId);

	boolean existsByNickname(String nickname);
}
