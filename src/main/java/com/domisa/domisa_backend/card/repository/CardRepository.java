package com.domisa.domisa_backend.card.repository;

import com.domisa.domisa_backend.card.entity.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

	Optional<Card> findByUserId(Long userId);

	@Query("""
		select c
		from Card c
		join fetch c.user u
		left join fetch u.profileImage
		where u.id = :userId
		""")
	Optional<Card> findByUserIdWithUserAndProfileImage(@Param("userId") Long userId);

	void deleteByUserId(Long userId);
}
