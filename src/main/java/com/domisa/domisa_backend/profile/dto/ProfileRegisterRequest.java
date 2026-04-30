package com.domisa.domisa_backend.profile.dto;

import com.domisa.domisa_backend.user.dto.ContactDTO;
import com.domisa.domisa_backend.user.type.AnimalProfile;

public record ProfileRegisterRequest(
        String nickname,
        Boolean gender,
        Long birthYear,
        ContactDTO contact,
        AnimalProfile animalProfile
) {}
