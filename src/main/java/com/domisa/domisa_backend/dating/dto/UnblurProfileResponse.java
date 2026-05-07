package com.domisa.domisa_backend.dating.dto;

public record UnblurProfileResponse(
	String publicId,
	boolean isBlurred,
	String message
) {
}
