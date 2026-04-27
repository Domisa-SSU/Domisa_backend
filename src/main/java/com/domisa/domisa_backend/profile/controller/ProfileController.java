package com.domisa.domisa_backend.profile.controller;

import com.domisa.domisa_backend.domain.user.entity.User;
import com.domisa.domisa_backend.domain.user.repository.UserRepository;
import com.domisa.domisa_backend.global.dto.ErrorResponse;
import com.domisa.domisa_backend.profile.dto.ProfileRegisterRequest;
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

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) {
        if (nickname.length() > 4) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse(400, "INVALID_NICKNAME_LENGTH", "닉네임은 최대 4글자까지 입력 가능합니다.")
            );
        }
        boolean isAvailable = !userRepository.existsByNickname(nickname);
        return ResponseEntity.ok(Map.of("isAvailable", isAvailable));
    }

    @PostMapping("/me")
    public ResponseEntity<?> registerProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody ProfileRegisterRequest request) {

        if (request.nickName() == null || request.nickName().isBlank()) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse(400, "MISSING_REQUIRED_FIELD", "필수 입력 항목이 누락되었습니다.")
            );
        }

        if (request.nickName().length() > 4) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse(400, "INVALID_NICKNAME_LENGTH", "닉네임은 최대 4글자까지 입력 가능합니다.")
            );
        }

        if (userRepository.existsByNickname(request.nickName())) {
            return ResponseEntity.status(409).body(
                new ErrorResponse(409, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.")
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        user.registerProfile(request.toCommand());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("userId", user.getId()));
    }
}
