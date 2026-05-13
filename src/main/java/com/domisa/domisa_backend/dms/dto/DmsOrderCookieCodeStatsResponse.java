package com.domisa.domisa_backend.dms.dto;

public record DmsOrderCookieCodeStatsResponse(
	String cookieCode,
	String unitDescription,
	long orderCount,
	long cookieAmount,
	long orderAmount,
	long paidOrderAmount
) {
}
