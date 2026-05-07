package com.domisa.domisa_backend.dating.dto;

public record DatingProfileDetailRequest(
	ViewType viewType
) {
	public enum ViewType {
		NORMAL, // 일반 프로필 상세 조회 (사진만 블러)
		FAN     // 받은 호감에서 조회 (사진 + q3 + idealType 블러)
	}
}
