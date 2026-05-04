package com.domisa.domisa_backend.profileimage.repository;

import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.type.ProfileImageProcessingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {

	Optional<ProfileImage> findByUserId(Long userId);

	void deleteByUserId(Long userId);

	List<ProfileImage> findByProcessingStatusInOrderByIdAsc(
		Collection<ProfileImageProcessingStatus> statuses,
		Pageable pageable
	);
}
