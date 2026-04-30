package com.domisa.domisa_backend.profileimage.type;

public enum ProfileImageVariant {
	ORIGIN("origin", "original"),
	THUMBNAIL("thumbnail", "original"),
	THUMBNAIL_BLUR("thumbnail", "blur"),
	ORIGIN_BLUR("origin", "blur");

	private final String directory;
	private final String fileName;

	ProfileImageVariant(String directory, String fileName) {
		this.directory = directory;
		this.fileName = fileName;
	}

	public String directory() {
		return directory;
	}

	public String fileName() {
		return fileName;
	}
}
