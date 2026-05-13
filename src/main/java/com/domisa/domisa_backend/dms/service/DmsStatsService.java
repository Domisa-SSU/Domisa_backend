package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.dms.dto.DmsStatsResponse;
import com.domisa.domisa_backend.payment.entity.CookieCode;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DmsStatsService {

	private static final LocalTime DAY_BOUNDARY = LocalTime.of(8, 0);
	private static final DateTimeFormatter RANGE_FORMAT = DateTimeFormatter.ofPattern("M/d HH:mm");
	private static final LocalDate BASE_DAY = LocalDate.of(2026, 5, 13);

	private final UserRepository userRepository;
	private final DmsOrderService dmsOrderService;

	@Transactional(readOnly = true)
	public DmsStatsResponse getStats() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime day1Start = BASE_DAY.atTime(DAY_BOUNDARY);
		LocalDateTime day2Start = day1Start.plusDays(1);
		LocalDateTime day3Start = day2Start.plusDays(1);
		LocalDateTime day4Start = day3Start.plusDays(1);

		List<DmsStatsResponse.DayStat> dayStats = new ArrayList<>(4);
		dayStats.add(buildDayStat("1일차 (2026-05-13)", day1Start, day2Start));
		dayStats.add(buildDayStat("2일차 (2026-05-14)", day2Start, day3Start));
		dayStats.add(buildDayStat("3일차 (2026-05-15)", day3Start, day4Start));
		dayStats.add(buildDayStat("나머지 외 시간", day4Start, now));
		return new DmsStatsResponse(dayStats);
	}

	private DmsStatsResponse.DayStat buildDayStat(String label, LocalDateTime start, LocalDateTime end) {
		if (end.isBefore(start)) {
			end = start;
		}
		long male = userRepository.countCompletedUsersByCreatedAtBetween(start, end, true);
		long female = userRepository.countCompletedUsersByCreatedAtBetween(start, end, false);
		long total = userRepository.countCompletedUsersByCreatedAtBetween(start, end, null);

		List<CookieOrder> completedOrders = dmsOrderService.findCompletedOrdersBetween(start, end);
		long totalOrderCount = completedOrders.size();
		long totalOrderAmount = completedOrders.stream().mapToLong(CookieOrder::getOrderAmount).sum();

		Map<CookieCode, Long> codeCounts = new EnumMap<>(CookieCode.class);
		Arrays.stream(CookieCode.values()).forEach(code -> codeCounts.put(code, 0L));
		completedOrders.forEach(order -> {
			CookieCode code = resolveCookieCode(order);
			if (code != null) {
				codeCounts.put(code, codeCounts.get(code) + 1);
			}
		});

		List<DmsStatsResponse.OrderCodeStat> orderCodeStats = Arrays.stream(CookieCode.values())
			.map(code -> new DmsStatsResponse.OrderCodeStat(
				code.name(),
				code.getOrderAmount() + "원",
				codeCounts.get(code)
			))
			.toList();

		String rangeText = label.equals("나머지 외 시간")
			? RANGE_FORMAT.format(start) + " 이후"
			: RANGE_FORMAT.format(start) + " ~ " + RANGE_FORMAT.format(end);
		return new DmsStatsResponse.DayStat(
			label,
			rangeText,
			male,
			female,
			total,
			orderCodeStats,
			totalOrderCount,
			totalOrderAmount
		);
	}

	private CookieCode resolveCookieCode(CookieOrder order) {
		return Arrays.stream(CookieCode.values())
			.filter(code -> code.getCookieAmount() == order.getCookieAmount()
				&& code.getOrderAmount() == order.getOrderAmount())
			.findFirst()
			.orElse(null);
	}
}
