package com.domisa.domisa_backend.card.dto;

import com.domisa.domisa_backend.user.type.Mbti;

public record CardUpdateRequest(
        Mbti mbti,
        String datingStyle,
        String idealType,
        String imageKey
) {
}
