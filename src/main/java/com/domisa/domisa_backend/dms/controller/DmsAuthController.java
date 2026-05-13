package com.domisa.domisa_backend.dms.controller;

import com.domisa.domisa_backend.dms.service.DmsAuthService;
import com.domisa.domisa_backend.dms.config.DmsSessionKeys;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/dms-room")
@RequiredArgsConstructor
public class DmsAuthController {

	private final DmsAuthService dmsAuthService;

	@GetMapping("/login")
	public String loginPage(HttpSession session, HttpServletResponse response) {
		addNoStoreHeaders(response);
		if (Boolean.TRUE.equals(session.getAttribute(DmsSessionKeys.DMS_ADMIN_LOGIN))) {
			return "redirect:/dms-room/users";
		}
		return "dms/login";
	}

	@PostMapping("/login")
	public String login(
		@RequestParam("loginId") String loginId,
		@RequestParam("password") String password,
		HttpSession session,
		HttpServletResponse response,
		Model model
	) {
		addNoStoreHeaders(response);
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

	@GetMapping("/session/ping")
	@ResponseBody
	public ResponseEntity<Void> ping() {
		return ResponseEntity.noContent().build();
	}

	private void addNoStoreHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
	}
}
