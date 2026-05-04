package com.domisa.domisa_backend.profile.dto;

import com.domisa.domisa_backend.user.type.AnimalProfile;

public record ProfileUpdateResponse(
        Long userId,
        String nickname,
        Boolean gender,
        Long birthYear,
        AnimalProfile animalProfile
) {}
