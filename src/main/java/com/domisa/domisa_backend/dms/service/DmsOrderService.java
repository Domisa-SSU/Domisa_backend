package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.dms.dto.DmsOrderCookieCodeStatsResponse;
import com.domisa.domisa_backend.dms.dto.DmsOrderListResponse;
import com.domisa.domisa_backend.dms.dto.DmsOrderStatsResponse;
import com.domisa.domisa_backend.payment.entity.CookieCode;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.payment.service.CookieOrderService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DmsOrderService {

	private static final List<OrderStatus> DMS_VISIBLE_ORDER_STATUSES = List.of(
		OrderStatus.PAID,
		OrderStatus.ALREADY_PROCESSED
	);

	private final CookieOrderRepository cookieOrderRepository;
	private final CookieOrderService cookieOrderService;

	@Transactional(readOnly = true)
	public DmsOrderListResponse getOrders(OrderStatus statusFilter) {
		List<OrderStatus> statuses = statusFilter == null
			? Arrays.asList(OrderStatus.values())
			: List.of(statusFilter);
		List<CookieOrder> orders = cookieOrderRepository.findAllForDms(statuses);
		return new DmsOrderListResponse(
			buildStats(orders),
			buildCookieCodeStats(orders),
			orders.stream().map(this::toRow).toList()
		);
	}

	@Transactional
	public void updateOrderStatus(Long orderId, OrderStatus status) {
		CookieOrder order = cookieOrderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

		boolean shouldExcludePayAction = order.getStatus() == OrderStatus.PENDING
			&& (status == OrderStatus.PAID || status == OrderStatus.ALREADY_PROCESSED);
		if (shouldExcludePayAction) {
			cookieOrderService.excludePayActionOrderByOrderNumber(order.getOrderNumber());
		}
		order.updateStatusByAdmin(status);
	}

	@Transactional(readOnly = true)
	public List<CookieOrder> findCompletedOrdersBetween(LocalDateTime start, LocalDateTime end) {
		return cookieOrderRepository.findAllByStatusesAndPaidAtBetween(DMS_VISIBLE_ORDER_STATUSES, start, end);
	}

	public Collection<OrderStatus> getEditableStatuses() {
		return Arrays.asList(OrderStatus.values());
	}

	private DmsOrderStatsResponse buildStats(List<CookieOrder> orders) {
		long totalCookieAmount = orders.stream().mapToLong(CookieOrder::getCookieAmount).sum();
		long totalOrderAmount = orders.stream().mapToLong(CookieOrder::getOrderAmount).sum();
		long paidOrderAmount = orders.stream()
			.filter(CookieOrder::isPaid)
			.mapToLong(CookieOrder::getOrderAmount)
			.sum();
		long paidOrders = orders.stream().filter(CookieOrder::isPaid).count();
		long pendingOrders = orders.stream().filter(order -> order.getStatus() == OrderStatus.PENDING).count();
		long failedOrCanceledOrders = orders.stream()
			.filter(order -> order.getStatus() == OrderStatus.FAILED || order.getStatus() == OrderStatus.CANCELED)
			.count();

		return new DmsOrderStatsResponse(
			orders.size(),
			totalCookieAmount,
			totalOrderAmount,
			paidOrderAmount,
			paidOrders,
			pendingOrders,
			failedOrCanceledOrders
		);
	}

	private List<DmsOrderCookieCodeStatsResponse> buildCookieCodeStats(List<CookieOrder> orders) {
		Map<CookieCode, MutableCookieCodeStats> stats = new EnumMap<>(CookieCode.class);
		Arrays.stream(CookieCode.values()).forEach(code -> stats.put(code, new MutableCookieCodeStats()));

		orders.forEach(order -> {
			CookieCode code = resolveCookieCode(order);
			if (code == null) {
				return;
			}
			stats.get(code).add(order);
		});

		return stats.entrySet().stream()
			.map(entry -> entry.getValue().toResponse(entry.getKey()))
			.toList();
	}

	private DmsOrderListResponse.OrderRow toRow(CookieOrder order) {
		CookieCode cookieCode = resolveCookieCode(order);
		return new DmsOrderListResponse.OrderRow(
			order.getId(),
			order.getUser().getId(),
			order.getUser().getPublicId(),
			order.getBillingName(),
			order.getOrderAmount(),
			cookieCode == null ? "-" : cookieCode.name(),
			cookieCode == null ? "-" : formatUnitDescription(cookieCode),
			order.getCookieAmount(),
			order.getStatus().name(),
			order.getOrderDate(),
			order.getPaidAt(),
			order.getOrderNumber()
		);
	}

	private CookieCode resolveCookieCode(CookieOrder order) {
		return Arrays.stream(CookieCode.values())
			.filter(code -> code.getCookieAmount() == order.getCookieAmount()
				&& code.getOrderAmount() == order.getOrderAmount())
			.findFirst()
			.orElse(null);
	}

	private String formatUnitDescription(CookieCode code) {
		return code.getOrderAmount() + "원에 " + code.getCookieAmount() + "개";
	}

	private class MutableCookieCodeStats {
		private long orderCount;
		private long cookieAmount;
		private long orderAmount;
		private long paidOrderAmount;

		private void add(CookieOrder order) {
			orderCount++;
			cookieAmount += order.getCookieAmount();
			orderAmount += order.getOrderAmount();
			if (order.isPaid()) {
				paidOrderAmount += order.getOrderAmount();
			}
		}

			private DmsOrderCookieCodeStatsResponse toResponse(CookieCode code) {
				return new DmsOrderCookieCodeStatsResponse(
					code.name(),
					formatUnitDescription(code),
					orderCount,
					cookieAmount,
					orderAmount,
					paidOrderAmount
				);
		}
	}
}
