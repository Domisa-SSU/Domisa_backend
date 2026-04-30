package com.domisa.domisa_backend.user.repository;

import com.domisa.domisa_backend.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKakaoId(Long kakaoId);

	Optional<User> findByPublicId(String publicId);

	@EntityGraph(attributePaths = "profileImage")
	Optional<User> findWithProfileImageById(Long id);

	@EntityGraph(attributePaths = "profileImage")
	Optional<User> findWithProfileImageByPublicId(String publicId);

	@EntityGraph(attributePaths = {"profileImage", "card", "introduction"})
	Optional<User> findDatingProfileById(Long id);

	@EntityGraph(attributePaths = {"profileImage", "card", "introduction"})
	Optional<User> findDatingProfileByPublicId(String publicId);

	@EntityGraph(attributePaths = "profileImage")
	List<User> findAllByIdIn(Collection<Long> ids);

	boolean existsByNickname(String nickname);

	boolean existsByPublicId(String publicId);
}
