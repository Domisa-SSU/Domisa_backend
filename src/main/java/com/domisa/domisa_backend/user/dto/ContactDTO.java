package com.domisa.domisa_backend.user.dto;

import com.domisa.domisa_backend.user.type.ContactType;

// 연락처랑 타입이랑 묶는 용도
public record ContactDTO(
        ContactType type,
        String content
) {}
