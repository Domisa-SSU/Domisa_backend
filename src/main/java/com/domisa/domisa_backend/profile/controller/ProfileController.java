package com.domisa.domisa_backend.profile.controller;

import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
import com.domisa.domisa_backend.profile.service.ProfileService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(profileService.checkNickname(nickname));
    }

    @PostMapping("/me")
    public ResponseEntity<Map<String, Long>> registerProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody ProfileRegisterRequest request) {
        return ResponseEntity.ok(profileService.registerProfile(userId, request));
    }
}
