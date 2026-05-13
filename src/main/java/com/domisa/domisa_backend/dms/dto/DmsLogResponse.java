package com.domisa.domisa_backend.dms.dto;

public record DmsLogResponse(
	String filePath,
	int lines,
	String content,
	String errorMessage,
	boolean loaded
) {

	public boolean hasError() {
		return errorMessage != null && !errorMessage.isBlank();
	}
}
