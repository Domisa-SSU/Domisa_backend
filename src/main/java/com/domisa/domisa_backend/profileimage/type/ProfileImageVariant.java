package com.domisa.domisa_backend.profileimage.type;

public enum ProfileImageVariant {
	SOURCE("source", "original"),
	THUMBNAIL("thumbnail", "original"),
	THUMBNAIL_BLUR("thumbnail", "blur"),
	DETAIL_BLUR("detail", "blur");

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
