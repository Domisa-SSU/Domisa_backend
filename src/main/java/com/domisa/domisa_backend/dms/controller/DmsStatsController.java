package com.domisa.domisa_backend.dms.controller;

import com.domisa.domisa_backend.dms.service.DmsStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dms-room/stats")
@RequiredArgsConstructor
public class DmsStatsController {

	private final DmsStatsService dmsStatsService;

	@GetMapping
	public String stats(Model model) {
		model.addAttribute("page", dmsStatsService.getStats());
		return "dms/stats";
	}
}
