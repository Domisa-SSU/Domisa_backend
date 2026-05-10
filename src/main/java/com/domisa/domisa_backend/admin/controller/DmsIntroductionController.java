package com.domisa.domisa_backend.admin.controller;

import com.domisa.domisa_backend.admin.service.DmsIntroductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dms-room/introductions")
@RequiredArgsConstructor
public class DmsIntroductionController {

	private final DmsIntroductionService dmsIntroductionService;

	@GetMapping
	public String introductions(Model model) {
		model.addAttribute("page", dmsIntroductionService.getIntroductions());
		return "dms/introductions";
	}

	@PostMapping("/{introductionId}/delete")
	public String deleteIntroduction(
		@PathVariable Long introductionId,
		RedirectAttributes redirectAttributes
	) {
		dmsIntroductionService.deleteIntroduction(introductionId);
		redirectAttributes.addFlashAttribute("message", "Introduction을 삭제했습니다.");
		return "redirect:/dms-room/introductions";
	}
}
