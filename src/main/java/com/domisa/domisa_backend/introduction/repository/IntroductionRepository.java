package com.domisa.domisa_backend.introduction.repository;

import com.domisa.domisa_backend.introduction.entity.Introduction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntroductionRepository extends JpaRepository<Introduction, Long> {
	Optional<Introduction> findByLinkCode(String linkCode);
}
