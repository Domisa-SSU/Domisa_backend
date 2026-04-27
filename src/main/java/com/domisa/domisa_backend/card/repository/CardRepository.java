package com.domisa.domisa_backend.card.repository;

import com.domisa.domisa_backend.card.entity.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

	Optional<Card> findByUserId(Long userId);
}
