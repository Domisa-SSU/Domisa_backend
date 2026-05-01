package com.domisa.domisa_backend.dating.dto;

public record LikeSendResponse(
        boolean usedFreeChance,
        long remainingCookies
) {}
