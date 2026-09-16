package com.template.core.user.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.template.core.common.response.ApiResponse;
import com.template.core.user.service.UserService;
import com.template.core.user.dto.LoginRequest;
import com.template.core.user.dto.LoginResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 인증(로그인) 컨트롤러.
 *
 * <p>로그인 성공 시 JWT를 발급해 반환한다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    /** 로그인 후 JWT를 발급한다. */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }
}
