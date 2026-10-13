package com.firstpress.product.controller;

import com.firstpress.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health-check")
    public ResponseEntity<ApiResponse<Void>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success());
    }
}
