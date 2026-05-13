package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.dms.dto.DmsStatsResponse;
import com.domisa.domisa_backend.payment.entity.CookieCode;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

	private static final int DEFAULT_DAYS = 7;
	private static final LocalTime DAY_BOUNDARY = LocalTime.of(8, 0);
	private static final DateTimeFormatter RANGE_FORMAT = DateTimeFormatter.ofPattern("M/d HH:mm");

	private final UserRepository userRepository;
	private final DmsOrderService dmsOrderService;

	@Transactional(readOnly = true)
	public DmsStatsResponse getStats() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime cycleStart = now.toLocalTime().isBefore(DAY_BOUNDARY)
			? LocalDate.now().minusDays(1).atTime(DAY_BOUNDARY)
			: LocalDate.now().atTime(DAY_BOUNDARY);

		List<DmsStatsResponse.DayStat> dayStats = new ArrayList<>();
		for (int i = DEFAULT_DAYS - 1; i >= 0; i--) {
			LocalDateTime start = cycleStart.minusDays(i);
			LocalDateTime end = start.plusDays(1);
			dayStats.add(buildDayStat(start, end));
		}
		return new DmsStatsResponse(dayStats);
	}

	private DmsStatsResponse.DayStat buildDayStat(LocalDateTime start, LocalDateTime end) {
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
				code.getOrderAmount() + "원 / " + code.getCookieAmount() + "개",
				codeCounts.get(code)
			))
			.toList();

		String dayLabel = start.toLocalDate().toString();
		String rangeText = RANGE_FORMAT.format(start) + " ~ " + RANGE_FORMAT.format(end);
		return new DmsStatsResponse.DayStat(
			dayLabel,
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
