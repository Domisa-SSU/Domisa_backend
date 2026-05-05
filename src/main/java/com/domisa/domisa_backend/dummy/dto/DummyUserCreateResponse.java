package com.domisa.domisa_backend.dummy.dto;

import java.util.List;

public record DummyUserCreateResponse(
	int requestedCount,
	int createdCount,
	int totalCount,
	List<DummyUserResponse> users
) {
}
