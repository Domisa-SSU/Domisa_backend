package com.domisa.domisa_backend.card.dto;

public record CardCreateResponse(
        Long userId,
        StatusDto status
) {
    public record StatusDto(
            boolean isRegistered,
            boolean hasIntroduction,
            boolean isCardCompleted
    ){}
}
