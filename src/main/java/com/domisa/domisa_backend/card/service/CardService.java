package com.domisa.domisa_backend.card.service;

import com.domisa.domisa_backend.card.dto.CardCreateRequest;
import com.domisa.domisa_backend.card.dto.CardCreateResponse;
import com.domisa.domisa_backend.card.dto.CardCreateResponse.StatusDto;
import com.domisa.domisa_backend.card.dto.CardResponse;
import com.domisa.domisa_backend.card.dto.CardUpdateRequest;
import com.domisa.domisa_backend.card.entity.Card;
import com.domisa.domisa_backend.card.repository.CardRepository;
import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    // 소개팅 카드 생성
    @Transactional
    public CardCreateResponse createCard(Long userId, CardCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

        // 회원가입 완료 여부 체크
        if (!user.getIsRegistered()) {
            throw new GlobalException(GlobalErrorCode.USER_NOT_REGISTERED);
        }

        // 소개서 수락 여부 체크
        if (!user.hasIntroduction()) {
            throw new GlobalException(GlobalErrorCode.INTRODUCTION_NOT_FOUND);
        }

        // 이미 카드가 있을 경우에는 불가능
        if(cardRepository.findByUserId(userId).isPresent()) {
            throw new GlobalException(GlobalErrorCode.CARD_ALREADY_EXISTS);
        }

        Card card = Card.create(
                user,
                request.mbti(),
                request.datingStyle(),
                request.idealType(),
                request.imageKey()
        );
        cardRepository.save(card);

        // 연락처 저장
        user.setContactType(request.contactType());
        user.setContact(request.contact());
        user.setNotificationPhone(request.notificationPhone());

        // 카드 생성 완료
        user.setIsProfileCompleted(true);

        return new CardCreateResponse(
                user.getPublicId(),
                new StatusDto(user.getIsRegistered(),
                        user.hasIntroduction(),
                        user.hasCard()
                )
        );
    }

    // 소개팅 카드 조회
    @Transactional(readOnly = true)
    public CardResponse getCard(Long userId) {
        Card card = cardRepository.findByUserId(userId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.CARD_NOT_FOUND));

        return new CardResponse(
                card.getId(),
                card.getMbti(),
                card.getDatingStyle(),
                card.getIdealType(),
                card.getImageKey()
        );
    }

    // 소개팅 카드 수정
    @Transactional
    public CardResponse updateCard(Long userId, CardUpdateRequest request) {
        Card card = cardRepository.findByUserId(userId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.CARD_NOT_FOUND));

        card.update(
                request.mbti(),
                request.datingStyle(),
                request.idealType(),
                request.imageKey()
        );

        // 연락처 수정
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
        user.setContactType(request.contactType());
        user.setContact(request.contact());
        user.setNotificationPhone(request.notificationPhone());

        return new CardResponse(
                card.getId(),
                card.getMbti(),
                card.getDatingStyle(),
                card.getIdealType(),
                card.getImageKey()
        );
    }

}
