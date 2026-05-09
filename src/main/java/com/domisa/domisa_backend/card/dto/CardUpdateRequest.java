package com.domisa.domisa_backend.card.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.domisa.domisa_backend.user.type.ContactType;
import com.domisa.domisa_backend.user.type.Mbti;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardUpdateRequest(
        Mbti mbti,
        String datingStyle,
        String idealType,
        ContactType contactType,
        String contact,
        String notificationPhone // 알림 받을 전화번호 (문자 안받을래요 체크 시 null)
) {}
