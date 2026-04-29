package com.domisa.domisa_backend.profileimage.dto;

public record ProcessedProfileImageSet(
	byte[] thumbnail,
	byte[] thumbnailBlur,
	byte[] originBlur
) {
}
