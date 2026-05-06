package com.domisa.domisa_backend.card.dto;

public record CardCreateResponse(
        String publicId,
        StatusDto status
) {
    public record StatusDto(
            boolean isRegistered,
            boolean hasIntroduction,
            boolean isCardCompleted
    ){}
}
