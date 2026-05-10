package com.domisa.domisa_backend.admin.service;

import com.domisa.domisa_backend.admin.dto.DmsOrderCookieCodeStatsResponse;
import com.domisa.domisa_backend.admin.dto.DmsOrderListResponse;
import com.domisa.domisa_backend.admin.dto.DmsOrderStatsResponse;
import com.domisa.domisa_backend.payment.entity.CookieCode;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DmsOrderService {

	private final CookieOrderRepository cookieOrderRepository;

	@Transactional(readOnly = true)
	public DmsOrderListResponse getOrders() {
		List<CookieOrder> orders = cookieOrderRepository.findAllForDms();
		return new DmsOrderListResponse(
			buildStats(orders),
			buildCookieCodeStats(orders),
			orders.stream().map(this::toRow).toList()
		);
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
			order.getOrderNumber(),
			order.getUser().getId(),
			order.getUser().getPublicId(),
			order.getBillingName(),
			order.getOrderAmount(),
			cookieCode == null ? "-" : cookieCode.name(),
			order.getCookieAmount(),
			order.getStatus().name(),
			order.getOrderDate(),
			order.getPaidAt(),
			order.getCreatedAt(),
			order.getUpdatedAt()
		);
	}

	private CookieCode resolveCookieCode(CookieOrder order) {
		return Arrays.stream(CookieCode.values())
			.filter(code -> code.getCookieAmount() == order.getCookieAmount()
				&& code.getOrderAmount() == order.getOrderAmount())
			.findFirst()
			.orElse(null);
	}

	private static class MutableCookieCodeStats {
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
				orderCount,
				cookieAmount,
				orderAmount,
				paidOrderAmount
			);
		}
	}
}
