package com.domisa.domisa_backend.dummy.dto;

import java.util.List;

public record DummyUserListResponse(int totalCount, List<DummyUserResponse> users) {
}
