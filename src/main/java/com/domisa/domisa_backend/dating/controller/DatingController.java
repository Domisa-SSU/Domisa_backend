package com.domisa.domisa_backend.dating.controller;

import com.domisa.domisa_backend.auth.annotation.AuthUser;
import com.domisa.domisa_backend.dating.dto.DatingMatchCountResponse;
import com.domisa.domisa_backend.dating.dto.DatingIntroductionLinkCreateRequest;
import com.domisa.domisa_backend.dating.dto.DatingIntroductionLinkCreateResponse;
import com.domisa.domisa_backend.dating.dto.DatingProfileListResponse;
import com.domisa.domisa_backend.dating.dto.DatingProfileResponse;
import com.domisa.domisa_backend.dating.dto.DatingRefreshTimeResponse;
import com.domisa.domisa_backend.dating.dto.UnblurProfileResponse;
import com.domisa.domisa_backend.dating.service.DatingService;
import com.domisa.domisa_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datings")
@RequiredArgsConstructor
public class DatingController {

	private final DatingService datingService;

	@GetMapping("/profiles")
	public ResponseEntity<DatingProfileListResponse> getDatingProfiles(@AuthUser User authUser) {
		return ResponseEntity.ok(datingService.getDatingProfiles(authUser));
	}

	@GetMapping("/profiles/{publicId}")
	public ResponseEntity<DatingProfileResponse> getDatingProfile(
		@AuthUser User authUser,
		@PathVariable String publicId
	) {
		return ResponseEntity.ok(datingService.getDatingProfile(authUser, publicId));
	}

	@GetMapping("/refresh-time")
	public ResponseEntity<DatingRefreshTimeResponse> getRefreshTime(@AuthUser User authUser) {
		return ResponseEntity.ok(datingService.getDatingRefreshTime(authUser));
	}

	@PostMapping("/introduction-links")
	public ResponseEntity<DatingIntroductionLinkCreateResponse> createIntroductionLink(
		@AuthUser User authUser,
		@RequestBody DatingIntroductionLinkCreateRequest request
	) {
		return ResponseEntity.ok(datingService.createIntroductionLink(authUser, request));
	}

	@GetMapping("/count")
	public ResponseEntity<DatingMatchCountResponse> getMatchCount() {
		return ResponseEntity.ok(datingService.getMatchCount());
	}

	@PostMapping("/shuffle")
	public ResponseEntity<Void> shuffle(@AuthUser User authUser) {
		datingService.shuffle(authUser);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/profiles/{publicId}/unblur")
	public ResponseEntity<UnblurProfileResponse> unblurProfile(
		@AuthUser User authUser,
		@PathVariable String publicId
	) {
		return ResponseEntity.ok(datingService.unblurProfile(authUser, publicId));
	}

	@PostMapping("/likes/{publicId}")
	public ResponseEntity<Void> sendLike(
		@AuthUser User authUser,
		@PathVariable String publicId
	) {
		datingService.sendLike(authUser, publicId);
		return ResponseEntity.ok().build();
	}
}
