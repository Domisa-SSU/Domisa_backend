package com.domisa.domisa_backend.auth.dto;

public record LoginResponse(StatusDto status) {

    public record StatusDto(
            boolean isRegistered,
            boolean hasIntroduction,
            boolean isProfileCompleted
    ) {}
}
