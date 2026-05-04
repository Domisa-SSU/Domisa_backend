package com.domisa.domisa_backend.card.dto;

import com.domisa.domisa_backend.user.type.ContactType;
import com.domisa.domisa_backend.user.type.Mbti;

public record CardCreateRequest(
        Mbti mbti,
        String datingStyle,
        String idealType,
        String imageKey,
        ContactType contactType,
        String contact,
        String notificationPhone // 알림 받을 전화번호 (문자 안받을래요 체크 시 null)
) {}
