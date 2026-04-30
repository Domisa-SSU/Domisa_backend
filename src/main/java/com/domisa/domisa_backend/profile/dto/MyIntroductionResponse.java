package com.domisa.domisa_backend.profile.dto;

import com.domisa.domisa_backend.introduction.entity.Introduction;

public record MyIntroductionResponse(
	String q1,
	String q2,
	String q3
) {

	public static MyIntroductionResponse from(Introduction introduction) {
		return new MyIntroductionResponse(
			introduction.getQ1(),
			introduction.getQ2(),
			introduction.getQ3()
		);
	}
}
