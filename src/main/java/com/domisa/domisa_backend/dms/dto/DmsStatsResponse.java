package com.domisa.domisa_backend.dms.dto;

import java.util.List;

public record DmsStatsResponse(
	List<DayStat> days
) {
	public record DayStat(
		String label,
		String rangeText,
		long completedMaleUsers,
		long completedFemaleUsers,
		long completedTotalUsers,
		List<OrderCodeStat> orderCodeStats,
		long completedOrderCount,
		long completedOrderAmount
	) {
	}

	public record OrderCodeStat(
		String cookieCode,
		String unitDescription,
		long completedOrderCount
	) {
	}
}
