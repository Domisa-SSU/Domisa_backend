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
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
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
    private final S3ObjectUrlService s3ObjectUrlService;
    private final IntroductionRepository introductionRepository;

    // 소개팅 카드 생성
    @Transactional
    public CardCreateResponse createCard(User user, CardCreateRequest request) {
        validateAuthUser(user);

        // 회원가입 완료 여부 체크
        if (!user.getIsRegistered()) {
            throw new GlobalException(GlobalErrorCode.USER_NOT_REGISTERED);
        }

        // 이미 카드가 있을 경우에는 불가능
        if(cardRepository.findByUserId(user.getId()).isPresent()) {
            throw new GlobalException(GlobalErrorCode.CARD_ALREADY_EXISTS);
        }

        Card card = Card.create(
                user,
                request.mbti(),
                request.datingStyle(),
                request.idealType()
        );
        cardRepository.save(card);

        // 연락처 저장
        user.setContactType(request.contactType());
        user.setContact(request.contact());
        user.setNotificationPhone(normalizeNullableText(request.notificationPhone()));

        // 카드 생성 완료
        user.setIsProfileCompleted(true);

        return new CardCreateResponse(
                user.getPublicId(),
                new StatusDto(user.getIsRegistered(),
                        hasIntroduction(user.getId()),
                        user.hasCard()
                ),
                userRepository.count()
        );
    }

    private boolean hasIntroduction(Long userId) {
        return introductionRepository.existsByParticipantId(userId);
    }

    // 소개팅 카드 조회
    @Transactional(readOnly = true)
    public CardResponse getCard(User user) {
        validateAuthUser(user);
        Card card = cardRepository.findByUserIdWithUserAndProfileImage(user.getId())
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.CARD_NOT_FOUND));

        return new CardResponse(
                card.getId(),
                card.getMbti(),
                card.getDatingStyle(),
                card.getIdealType(),
                toImageUrl(card.getUser()),
                card.getUser().getContactType(),
                card.getUser().getContact(),
                card.getUser().getNotificationPhone()
        );
    }

    // 소개팅 카드 수정
    @Transactional
    public CardResponse updateCard(User user, CardUpdateRequest request) {
        validateAuthUser(user);
        Card card = cardRepository.findByUserIdWithUserAndProfileImage(user.getId())
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.CARD_NOT_FOUND));

        card.update(
                request.mbti(),
                request.datingStyle(),
                request.idealType()
        );

        User cardUser = card.getUser();
        cardUser.setContactType(request.contactType());
        cardUser.setContact(request.contact());
        cardUser.setNotificationPhone(normalizeNullableText(request.notificationPhone()));

        return new CardResponse(
                card.getId(),
                card.getMbti(),
                card.getDatingStyle(),
                card.getIdealType(),
                toImageUrl(cardUser),
                cardUser.getContactType(),
                cardUser.getContact(),
                cardUser.getNotificationPhone()
        );
    }

    private void validateAuthUser(User user) {
        if (user == null || user.getId() == null) {
            throw new GlobalException(GlobalErrorCode.USER_NOT_FOUND);
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String toImageUrl(User user) {
        if (user == null || user.getProfileImage() == null) {
            return null;
        }
        return s3ObjectUrlService.getProfileImagePresignedUrl(user.getProfileImage());
    }

}
