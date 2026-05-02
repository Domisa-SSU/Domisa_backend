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

	@org.springframework.data.jpa.repository.Query(
		value = """
			SELECT COUNT(*) FROM user_my_types a
			JOIN user_my_types b
			  ON a.user_id = b.target_user_id
			 AND a.target_user_id = b.user_id
			 AND a.user_id < b.user_id
			""",
		nativeQuery = true
	)
	long countMutualMatches();

	@org.springframework.data.jpa.repository.Query(
		value = """
			SELECT u.id FROM users u
			WHERE u.id != :userId
			AND u.is_profile_completed = true
			ORDER BY RAND()
			LIMIT :limit
			""",
		nativeQuery = true
	)
	List<Long> findRandomUserIds(@org.springframework.data.repository.query.Param("userId") Long userId,
		@org.springframework.data.repository.query.Param("limit") int limit);
}
