package com.domisa.domisa_backend.dating.dto;

import java.util.List;

public record DatingMatchListResponse(
	int matchCount,
	List<MatchSummary> matches
) {
	public record MatchSummary(
		String publicId,
		String nickname,
		String profile,
		String contactType,
		String contact
	) {
	}
}
