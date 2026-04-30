package com.domisa.domisa_backend.global.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

	@GetMapping("/health")
	public ResponseEntity<Map<String, String>> health() {
		// 로드밸런서가 서버 생존 여부를 확인할 때 사용한다.
		return ResponseEntity.ok(Map.of("status", "UP"));
	}
}
