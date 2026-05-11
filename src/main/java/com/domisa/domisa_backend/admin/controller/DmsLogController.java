package com.domisa.domisa_backend.admin.controller;

import com.domisa.domisa_backend.admin.service.DmsLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dms-room/logs")
@RequiredArgsConstructor
public class DmsLogController {

	private final DmsLogService dmsLogService;

	@GetMapping
	public String logs(
		@RequestParam(value = "lines", required = false) Integer lines,
		@RequestParam(value = "load", defaultValue = "false") boolean load,
		Model model
	) {
		model.addAttribute("page", load ? dmsLogService.getLogs(lines) : dmsLogService.emptyLogs(lines));
		return "dms/logs";
	}
}
