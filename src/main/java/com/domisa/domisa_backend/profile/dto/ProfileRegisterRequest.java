package com.domisa.domisa_backend.profile.dto;

import com.domisa.domisa_backend.user.type.AnimalProfile;

public record ProfileRegisterRequest(
        String nickName,
        Boolean gender,
        Long birthYear,
        AnimalProfile animalProfile,
        String inviteCode,
        String contact
) {
}
