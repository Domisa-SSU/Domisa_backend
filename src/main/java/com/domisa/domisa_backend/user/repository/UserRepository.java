package com.domisa.domisa_backend.user.repository;

import com.domisa.domisa_backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKakaoId(Long kakaoId);

	@EntityGraph(attributePaths = "profileImage")
	Optional<User> findWithProfileImageById(Long id);

	boolean existsByNickname(String nickname);
}
