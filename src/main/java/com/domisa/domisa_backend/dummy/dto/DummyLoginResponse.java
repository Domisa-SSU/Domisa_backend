package com.domisa.domisa_backend.dummy.dto;

import com.domisa.domisa_backend.auth.dto.LoginResponse;
import java.util.List;

public record DummyLoginResponse(
	String publicId,
	Long kakaoId,
	String nickname,
	LoginResponse.StatusDto status,
	List<String> issuedCookies
) {
}
