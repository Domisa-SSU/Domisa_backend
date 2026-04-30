package com.domisa.domisa_backend.card.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.card.dto.CardCreateRequest;
import com.domisa.domisa_backend.card.dto.CardCreateResponse;
import com.domisa.domisa_backend.card.dto.CardResponse;
import com.domisa.domisa_backend.card.dto.CardUpdateRequest;
import com.domisa.domisa_backend.card.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/profiles")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // 소개팅 카드 생성
    @PostMapping
    public ResponseEntity<CardCreateResponse> createCard(
            @AuthUser Long userId,
            @RequestBody CardCreateRequest request
            ) {
        return ResponseEntity.ok(cardService.createCard(userId, request));
    }

    // 소개팅 카드 조회
    @GetMapping
    public ResponseEntity<CardResponse> getCard(@AuthUser Long userId) {
        return ResponseEntity.ok(cardService.getCard(userId));
    }

    // 소개팅 카드 수정
    @PutMapping
    public ResponseEntity<CardResponse> updateCard(
            @AuthUser Long userId,
            @RequestBody CardUpdateRequest request
            ) {
        return ResponseEntity.ok(cardService.updateCard(userId, request));
    }
}
