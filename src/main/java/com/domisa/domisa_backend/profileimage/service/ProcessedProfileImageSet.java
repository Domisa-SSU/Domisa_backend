package com.domisa.domisa_backend.profileimage.service;

public record ProcessedProfileImageSet(
	byte[] thumbnail,
	byte[] thumbnailBlur,
	byte[] detailBlur
) {
}
