package com.domisa.domisa_backend.dms.controller;

import com.domisa.domisa_backend.dms.service.DmsUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/dms-room/users")
@RequiredArgsConstructor
public class DmsUserController {

	private final DmsUserService dmsUserService;

	@GetMapping
	public String users(
		@RequestParam(value = "checked", required = false) String checked,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "gender", required = false) String gender,
		@RequestParam(value = "birthYearSort", required = false) String birthYearSort,
		@RequestParam(value = "q", required = false) String keyword,
		@RequestParam(value = "page", required = false) Integer page,
		Model model
	) {
		model.addAttribute("page", dmsUserService.getUsers(checked, status, gender, birthYearSort, keyword, page, false));
		model.addAttribute("listPath", "/dms-room/users");
		model.addAttribute("listTitle", "유저 목록");
		return "dms/users";
	}

	@GetMapping("/completed")
	public String completedUsers(
		@RequestParam(value = "checked", required = false) String checked,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "gender", required = false) String gender,
		@RequestParam(value = "birthYearSort", required = false) String birthYearSort,
		@RequestParam(value = "q", required = false) String keyword,
		@RequestParam(value = "page", required = false) Integer page,
		Model model
	) {
		model.addAttribute("page", dmsUserService.getUsers(checked, status, gender, birthYearSort, keyword, page, true));
		model.addAttribute("listPath", "/dms-room/users/completed");
		model.addAttribute("listTitle", "완료 유저 목록");
		return "dms/users";
	}

	@GetMapping("/excel")
	public ResponseEntity<byte[]> exportUsersExcel(
		@RequestParam(value = "checked", required = false) String checked,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "gender", required = false) String gender,
		@RequestParam(value = "birthYearSort", required = false) String birthYearSort,
		@RequestParam(value = "q", required = false) String keyword,
		@RequestParam(value = "completedOnly", required = false, defaultValue = "false") boolean completedOnly
	) {
		byte[] excelFile = dmsUserService.exportUsersExcel(
			checked,
			status,
			gender,
			birthYearSort,
			keyword,
			completedOnly
		);
		String fileName = completedOnly ? "dms-completed-users.xlsx" : "dms-users.xlsx";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
		return ResponseEntity.ok()
			.headers(headers)
			.body(excelFile);
	}

	@GetMapping("/{userId}")
	public String userDetail(@PathVariable Long userId, Model model) {
		model.addAttribute("user", dmsUserService.getUserDetail(userId));
		return "dms/user-detail";
	}

	@PostMapping("/{userId}/heaven")
	public String heaven(
		@PathVariable Long userId,
		@RequestParam(value = "checked", required = false) String checked,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "gender", required = false) String gender,
		@RequestParam(value = "birthYearSort", required = false) String birthYearSort,
		@RequestParam(value = "q", required = false) String keyword,
		@RequestParam(value = "completedOnly", required = false, defaultValue = "false") boolean completedOnly,
		@RequestParam(value = "page", required = false) Integer page
	) {
		dmsUserService.markHeaven(userId);
		return redirectUsers(checked, status, gender, birthYearSort, keyword, completedOnly, page);
	}

	@PostMapping("/{userId}/hell")
	public String hell(
		@PathVariable Long userId,
		@RequestParam(value = "checked", required = false) String checked,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "gender", required = false) String gender,
		@RequestParam(value = "birthYearSort", required = false) String birthYearSort,
		@RequestParam(value = "q", required = false) String keyword,
		@RequestParam(value = "completedOnly", required = false, defaultValue = "false") boolean completedOnly,
		@RequestParam(value = "page", required = false) Integer page
	) {
		dmsUserService.markHell(userId);
		return redirectUsers(checked, status, gender, birthYearSort, keyword, completedOnly, page);
	}

	@GetMapping("/{userId}/student-card")
	public String studentCard(@PathVariable Long userId) {
		return "redirect:" + dmsUserService.getStudentCardPresignedUrl(userId);
	}

	@GetMapping("/{userId}/profile-image")
	public String profileImage(@PathVariable Long userId) {
		return "redirect:" + dmsUserService.getProfileImagePresignedUrl(userId);
	}

	@PostMapping("/{userId}/cookies/add")
	public String addCookies(
		@PathVariable Long userId,
		@RequestParam("amount") long amount,
		@RequestParam(value = "reason", required = false) String reason,
		RedirectAttributes redirectAttributes
	) {
		try {
			dmsUserService.addCookies(userId, amount, reason);
			redirectAttributes.addFlashAttribute("message", "쿠키를 지급했습니다.");
		} catch (RuntimeException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:/dms-room/users/" + userId;
	}

	@PostMapping("/{userId}/cookies/subtract")
	public String subtractCookies(
		@PathVariable Long userId,
		@RequestParam("amount") long amount,
		@RequestParam(value = "reason", required = false) String reason,
		RedirectAttributes redirectAttributes
	) {
		try {
			dmsUserService.subtractCookies(userId, amount, reason);
			redirectAttributes.addFlashAttribute("message", "쿠키를 차감했습니다.");
		} catch (RuntimeException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:/dms-room/users/" + userId;
	}

	@PostMapping("/cookies/add-all")
	public String addCookiesToAll(
		@RequestParam("amount") long amount,
		RedirectAttributes redirectAttributes
	) {
		try {
			int updatedCount = dmsUserService.addCookiesToAll(amount);
			redirectAttributes.addFlashAttribute("message", "전체 유저 " + updatedCount + "명에게 쿠키를 지급했습니다.");
		} catch (RuntimeException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:/dms-room/users";
	}

	@PostMapping("/{userId}/delete")
	public String deleteUser(
		@PathVariable Long userId,
		RedirectAttributes redirectAttributes
	) {
		try {
			dmsUserService.deleteUser(userId);
			redirectAttributes.addFlashAttribute("message", "회원탈퇴 처리를 완료했습니다.");
			return "redirect:/dms-room/users";
		} catch (RuntimeException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
			return "redirect:/dms-room/users/" + userId;
		}
	}

	@PostMapping("/{userId}/now-shows/refresh")
	public String refreshNowShows(
		@PathVariable Long userId,
		RedirectAttributes redirectAttributes
	) {
		try {
			dmsUserService.refreshNowShows(userId);
			redirectAttributes.addFlashAttribute("message", "현재 노출 중인 유저 목록을 새로고침했습니다.");
		} catch (RuntimeException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:/dms-room/users/" + userId;
	}

	private String redirectUsers(String checked, String status, String gender, String birthYearSort, String keyword, boolean completedOnly, Integer page) {
		StringBuilder redirect = new StringBuilder(completedOnly ? "redirect:/dms-room/users/completed" : "redirect:/dms-room/users");
		String separator = "?";
		if (checked != null && !checked.isBlank()) {
			redirect.append(separator).append("checked=").append(encode(checked));
			separator = "&";
		}
		if (status != null && !status.isBlank()) {
			redirect.append(separator).append("status=").append(encode(status));
			separator = "&";
		}
		if (gender != null && !gender.isBlank()) {
			redirect.append(separator).append("gender=").append(encode(gender));
			separator = "&";
		}
		if (birthYearSort != null && !birthYearSort.isBlank()) {
			redirect.append(separator).append("birthYearSort=").append(encode(birthYearSort));
			separator = "&";
		}
		if (keyword != null && !keyword.isBlank()) {
			redirect.append(separator).append("q=").append(encode(keyword.strip()));
			separator = "&";
		}
		if (page != null && page > 1) {
			redirect.append(separator).append("page=").append(page);
		}
		return redirect.toString();
	}

	private String encode(String value) {
		return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
	}
}
