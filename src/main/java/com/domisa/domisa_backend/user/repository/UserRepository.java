package com.domisa.domisa_backend.user.repository;

import com.domisa.domisa_backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKakaoId(Long kakaoId);

	Optional<User> findByPublicId(String publicId);

	List<User> findByKakaoIdBetweenOrderByKakaoIdAsc(Long startKakaoId, Long endKakaoId);

	@EntityGraph(attributePaths = "profileImage")
	Optional<User> findWithProfileImageById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdWithLock(@Param("id") Long id);

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
			SELECT COUNT(DISTINCT LEAST(m.user_id, m.target_user_id), GREATEST(m.user_id, m.target_user_id))
			FROM user_my_matches m
			WHERE m.user_id < m.target_user_id
			AND EXISTS (
				SELECT 1
				FROM user_my_matches reverse_m
				WHERE reverse_m.user_id = m.target_user_id
				AND reverse_m.target_user_id = m.user_id
			)
			""",
		nativeQuery = true
	)
	long countMutualMatches();

	@org.springframework.data.jpa.repository.Query(
		value = """
			SELECT u.id FROM users u
			WHERE u.id != :userId
			AND u.is_registered = true
			AND u.has_introduction = true
			AND u.is_profile_completed = true
			AND u.gender <> :gender
			ORDER BY RAND()
			LIMIT :limit
			""",
		nativeQuery = true
	)
	List<Long> findRandomOppositeGenderUserIds(@org.springframework.data.repository.query.Param("userId") Long userId,
		@org.springframework.data.repository.query.Param("gender") Boolean gender,
		@org.springframework.data.repository.query.Param("limit") int limit);

	@org.springframework.data.jpa.repository.Query(
		value = """
			SELECT u.id FROM users u
			WHERE u.id != :userId
			AND u.is_registered = true
			AND u.has_introduction = true
			AND u.is_profile_completed = true
			AND u.gender <> :gender
			AND u.id NOT IN (:excludedUserIds)
			ORDER BY RAND()
			LIMIT :limit
			""",
		nativeQuery = true
	)
	List<Long> findRandomOppositeGenderUserIdsExcluding(
		@org.springframework.data.repository.query.Param("userId") Long userId,
		@org.springframework.data.repository.query.Param("gender") Boolean gender,
		@org.springframework.data.repository.query.Param("excludedUserIds") Collection<Long> excludedUserIds,
		@org.springframework.data.repository.query.Param("limit") int limit
	);

	@Query("""
		select u from User u
		where u.isRegistered = true
		and u.hasIntroduction = true
		and u.isProfileCompleted = true
		and (u.refreshAvailableAt is null or u.refreshAvailableAt <= :now)
		""")
	List<User> findUsersReadyForNowShowRefresh(@Param("now") LocalDateTime now);

	@Modifying
	@Query(value = "delete from user_my_blurs where user_id = :userId or target_user_id = :userId", nativeQuery = true)
	void deleteBlurRelations(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from user_my_fans where user_id = :userId or target_user_id = :userId", nativeQuery = true)
	void deleteFanRelations(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from user_my_types where user_id = :userId or target_user_id = :userId", nativeQuery = true)
	void deleteTypeRelations(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from user_my_matches where user_id = :userId or target_user_id = :userId", nativeQuery = true)
	void deleteMatchRelations(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from user_now_shows where user_id = :userId or target_user_id = :userId", nativeQuery = true)
	void deleteNowShowRelations(@Param("userId") Long userId);
}
