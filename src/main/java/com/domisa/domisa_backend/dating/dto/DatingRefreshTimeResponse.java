package com.domisa.domisa_backend.dating.dto;

import java.time.LocalDateTime;

public record DatingRefreshTimeResponse(
	LocalDateTime refreshAvailableAt,
	boolean canRefresh
) {
}
