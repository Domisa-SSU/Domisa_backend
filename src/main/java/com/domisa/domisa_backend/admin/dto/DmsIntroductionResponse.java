package com.domisa.domisa_backend.admin.dto;

import java.util.List;

public record DmsIntroductionResponse(
	List<IntroductionRow> introductions
) {

	public record IntroductionRow(
		Long id,
		Long introducerId,
		String introducerPublicId,
		String introducerNickname,
		Long participantId,
		String participantPublicId,
		String participantNickname,
		String linkCode,
		String q1,
		String q2,
		String q3
	) {
	}
}
