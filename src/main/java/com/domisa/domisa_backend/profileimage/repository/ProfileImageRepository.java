package com.domisa.domisa_backend.profileimage.repository;

import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {

	Optional<ProfileImage> findByUserId(Long userId);

	void deleteByUserId(Long userId);

	@Query("""
		select p.user.id
		from ProfileImage p
		where p.user.id in :userIds
			and p.processingStatus in :statuses
			and p.profileOriginKey is not null
			and p.profileOriginKey <> ''
		""")
	List<Long> findAvailableProfileImageUserIds(
		@Param("userIds") Collection<Long> userIds,
		@Param("statuses") Collection<ProfileImageProcessingStatus> statuses
	);

	@Query("""
		select p.id
		from ProfileImage p
		where p.processingStatus = :pendingStatus
			or (
				p.processingStatus = :failedStatus
				and coalesce(p.retryCount, 0) < :maxRetryCount
			)
		order by p.id asc
		""")
	List<Long> findRetryableIdsOrderByIdAsc(
		@Param("pendingStatus") ProfileImageProcessingStatus pendingStatus,
		@Param("failedStatus") ProfileImageProcessingStatus failedStatus,
		@Param("maxRetryCount") int maxRetryCount,
		Pageable pageable
	);

	@Query("select p from ProfileImage p join fetch p.user where p.id = :id")
	Optional<ProfileImage> findByIdWithUser(@Param("id") Long id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ProfileImage p
		set p.processingStatus = :processingStatus,
			p.lastError = null
		where p.id = :id
			and (
				p.processingStatus = :pendingStatus
				or (
					p.processingStatus = :failedStatus
					and coalesce(p.retryCount, 0) < :maxRetryCount
				)
			)
		""")
	int markAsProcessing(
		@Param("id") Long id,
		@Param("processingStatus") ProfileImageProcessingStatus processingStatus,
		@Param("pendingStatus") ProfileImageProcessingStatus pendingStatus,
		@Param("failedStatus") ProfileImageProcessingStatus failedStatus,
		@Param("maxRetryCount") int maxRetryCount
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ProfileImage p
		set p.profileOriginKey = :profileOriginKey,
			p.profileOriginBlurKey = :profileOriginBlurKey,
			p.profileThumbnailKey = :profileThumbnailKey,
			p.profileThumbnailBlurKey = :profileThumbnailBlurKey,
			p.processingStatus = :completedStatus,
			p.retryCount = 0,
			p.lastError = null
		where p.id = :id
			and p.uploadSequence = :uploadSequence
			and p.processingStatus = :processingStatus
		""")
	int markAsCompleted(
		@Param("id") Long id,
		@Param("uploadSequence") Long uploadSequence,
		@Param("profileOriginKey") String profileOriginKey,
		@Param("profileOriginBlurKey") String profileOriginBlurKey,
		@Param("profileThumbnailKey") String profileThumbnailKey,
		@Param("profileThumbnailBlurKey") String profileThumbnailBlurKey,
		@Param("completedStatus") ProfileImageProcessingStatus completedStatus,
		@Param("processingStatus") ProfileImageProcessingStatus processingStatus
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ProfileImage p
		set p.processingStatus = :failedStatus,
			p.retryCount = coalesce(p.retryCount, 0) + 1,
			p.lastError = :lastError
		where p.id = :id
			and p.uploadSequence = :uploadSequence
			and p.processingStatus = :processingStatus
		""")
	int markAsFailed(
		@Param("id") Long id,
		@Param("uploadSequence") Long uploadSequence,
		@Param("lastError") String lastError,
		@Param("failedStatus") ProfileImageProcessingStatus failedStatus,
		@Param("processingStatus") ProfileImageProcessingStatus processingStatus
	);
}
