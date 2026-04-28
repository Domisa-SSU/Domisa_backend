package com.domisa.domisa_backend.introduction.dto;

import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record IntroductionResponse(
	@JsonProperty("introduction_id") Long introductionId,
	List<IntroductionItem> introductions
) {

	public static IntroductionResponse from(Introduction introduction) {
		return new IntroductionResponse(
			introduction.getId(),
			List.of(new IntroductionItem(
				introduction.getQ1(),
				introduction.getQ2(),
				introduction.getQ3()
			))
		);
	}

	public record IntroductionItem(
		String q1,
		String q2,
		String q3
	) {
	}
}
