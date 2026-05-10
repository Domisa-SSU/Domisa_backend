package com.domisa.domisa_backend.introduction.repository;

import com.domisa.domisa_backend.introduction.entity.Introduction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IntroductionRepository extends JpaRepository<Introduction, Long> {
	Optional<Introduction> findByLinkCode(String linkCode);

	boolean existsByParticipantId(Long participantId);

	List<Introduction> findAllByParticipantIdOrIntroducerId(Long participantId, Long introducerId);

	@Query("""
		select i
		from Introduction i
		left join fetch i.introducer
		left join fetch i.participant
		order by i.id desc
		""")
	List<Introduction> findAllForDms();
}
