package com.domisa.domisa_backend.dms.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DmsOrderListResponse(
	DmsOrderStatsResponse stats,
	List<DmsOrderCookieCodeStatsResponse> cookieCodeStats,
	List<OrderRow> orders
) {

	public record OrderRow(
		Long id,
		Long userId,
		String userPublicId,
		String billingName,
		Integer orderAmount,
		String cookieCode,
		String cookieUnitDescription,
		Integer cookieAmount,
		String orderStatus,
		LocalDateTime orderDate,
		LocalDateTime paidAt,
		String orderNumber
	) {
	}
}
