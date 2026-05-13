package com.domisa.domisa_backend.dms.controller;

import com.domisa.domisa_backend.dms.service.DmsOrderService;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dms-room/orders")
@RequiredArgsConstructor
public class DmsOrderController {

	private final DmsOrderService dmsOrderService;

	@GetMapping
	public String orders(Model model) {
		model.addAttribute("page", dmsOrderService.getOrders());
		model.addAttribute("statuses", dmsOrderService.getEditableStatuses());
		return "dms/orders";
	}

	@PostMapping("/{orderId}/status")
	public String updateStatus(
		@PathVariable Long orderId,
		@RequestParam("status") OrderStatus status,
		RedirectAttributes redirectAttributes
	) {
		try {
			dmsOrderService.updateOrderStatus(orderId, status);
			redirectAttributes.addFlashAttribute("message", "주문 상태를 변경했습니다.");
		} catch (RuntimeException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:/dms-room/orders";
	}
}
