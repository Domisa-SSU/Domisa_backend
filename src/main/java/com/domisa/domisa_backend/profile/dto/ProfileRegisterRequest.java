package com.domisa.domisa_backend.profile.dto;

import com.domisa.domisa_backend.domain.user.command.RegisterProfileCommand;

public record ProfileRegisterRequest(
        String nickName,
        Boolean gender,
        Long birthYear,
        String animalProfile,
        String inviteCode,
        String contact
) {
    public RegisterProfileCommand toCommand() {
        return new RegisterProfileCommand(
            nickName,
            gender,
            birthYear,
            animalProfile,
            contact,
            inviteCode
        );
    }
}
