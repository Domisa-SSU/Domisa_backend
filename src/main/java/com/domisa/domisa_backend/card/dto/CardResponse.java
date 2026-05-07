package com.domisa.domisa_backend.card.dto;

import com.domisa.domisa_backend.user.type.ContactType;
import com.domisa.domisa_backend.user.type.Mbti;

public record CardResponse(
        Long cardId,
        Mbti mbti,
        String datingStyle,
        String idealType,
        String imageKey,
        ContactType contactType,
        String contact,
        String notificationPhone
) {}
