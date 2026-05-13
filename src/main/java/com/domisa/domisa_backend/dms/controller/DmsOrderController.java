package com.domisa.domisa_backend.dms.controller;

import com.domisa.domisa_backend.dms.service.DmsOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dms-room/orders")
@RequiredArgsConstructor
public class DmsOrderController {

	private final DmsOrderService dmsOrderService;

	@GetMapping
	public String orders(Model model) {
		model.addAttribute("page", dmsOrderService.getOrders());
		return "dms/orders";
	}
}
