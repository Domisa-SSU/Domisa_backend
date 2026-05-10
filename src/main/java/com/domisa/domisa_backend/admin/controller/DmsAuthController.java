package com.domisa.domisa_backend.admin.controller;

import com.domisa.domisa_backend.admin.service.DmsAuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dms-room")
@RequiredArgsConstructor
public class DmsAuthController {

	private final DmsAuthService dmsAuthService;

	@GetMapping("/login")
	public String loginPage() {
		return "dms/login";
	}

	@PostMapping("/login")
	public String login(
		@RequestParam("loginId") String loginId,
		@RequestParam("password") String password,
		HttpSession session,
		Model model
	) {
		if (!dmsAuthService.login(loginId, password, session)) {
			model.addAttribute("errorMessage", "관리자 ID 또는 비밀번호가 올바르지 않습니다.");
			return "dms/login";
		}
		return "redirect:/dms-room/users";
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		dmsAuthService.logout(session);
		return "redirect:/dms-room/login";
	}
}
