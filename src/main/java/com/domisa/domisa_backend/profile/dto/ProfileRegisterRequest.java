package com.domisa.domisa_backend.profile.dto;

public record ProfileRegisterRequest(
        String nickName,
        Boolean gender,
        String birthYear,
        String animalProfile,
        String inviteCode,
        String contact
) {}
