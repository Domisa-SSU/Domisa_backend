package com.domisa.domisa_backend.user.dto;

import java.time.LocalDateTime;

public record DatingRefreshTimeResponse(
	LocalDateTime refreshAvailableAt,
	boolean canRefresh
) {
}
