package com.domisa.domisa_backend.dating.dto;

public record UnblurProfileResponse(
	String userId,
	boolean isBlurred,
	String message
) {
}
