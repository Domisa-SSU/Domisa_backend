package com.domisa.domisa_backend.admin.dto;

public record DmsOrderStatsResponse(
	long totalOrders,
	long totalCookieAmount,
	long totalOrderAmount,
	long paidOrderAmount,
	long paidOrders,
	long pendingOrders,
	long failedOrCanceledOrders
) {
}
