package com.domisa.domisa_backend.introduction.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.introduction.dto.IntroductionResponse;
import com.domisa.domisa_backend.introduction.service.IntroductionService;
import com.domisa.domisa_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class IntroductionController {

	private final IntroductionService introductionService;

	@GetMapping("/introduction/{linkCode}")
	public ResponseEntity<IntroductionResponse> getIntroduction(@PathVariable String linkCode) {
		return ResponseEntity.ok(introductionService.getIntroductionByLinkCode(linkCode));
	}

	@PostMapping("/users/introduction/{introductionId}")
	public ResponseEntity<Void> acceptIntroduction(
		@PathVariable Long introductionId,
		@AuthUser User user
	) {
		introductionService.acceptIntroduction(introductionId, user);
		return ResponseEntity.ok().build();
	}
}
