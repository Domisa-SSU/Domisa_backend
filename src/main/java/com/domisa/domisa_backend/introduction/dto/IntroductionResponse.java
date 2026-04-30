package com.domisa.domisa_backend.introduction.dto;

import com.domisa.domisa_backend.introduction.entity.Introduction;

public record IntroductionResponse(
	Long introductionId,
	String q1,
	String q2,
	String q3
) {

	public static IntroductionResponse from(Introduction introduction) {
		return new IntroductionResponse(
			introduction.getId(),
			introduction.getQ1(),
			introduction.getQ2(),
			introduction.getQ3()
		);
	}
}
