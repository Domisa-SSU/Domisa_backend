package com.domisa.domisa_backend.auth.blacklist.repository;

import com.domisa.domisa_backend.auth.blacklist.entity.UserBlacklist;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlacklistRepository extends JpaRepository<UserBlacklist, Long> {

	boolean existsByUserId(Long userId);

	Optional<UserBlacklist> findByUserId(Long userId);

	List<UserBlacklist> findByUserIdIn(Collection<Long> userIds);

	void deleteByUserId(Long userId);
}
