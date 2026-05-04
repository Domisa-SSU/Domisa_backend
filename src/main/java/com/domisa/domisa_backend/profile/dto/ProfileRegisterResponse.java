package com.domisa.domisa_backend.profile.dto;

public record ProfileRegisterResponse(
        Long userId,
        StatusDto status,
        long totalUserCount
) {
    public record StatusDto(
            boolean isRegistered,       // 회원가입 완료하면 true
            boolean hasIntroduction,    // 소개서 생성되면 true
            boolean isProfileCompleted  // 소개팅 카드 작성하면 true
    ) {}
}
