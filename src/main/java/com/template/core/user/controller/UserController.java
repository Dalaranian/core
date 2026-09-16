package com.template.core.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.template.core.common.response.ApiResponse;
import com.template.core.user.service.UserService;
import com.template.core.user.dto.UserJoinRequest;
import com.template.core.user.dto.UserJoinResponse;
import com.template.core.user.dto.WithdrawRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 회원 관리용 컨트롤러.
 *
 * <p>회원 가입과 탈퇴(본인 확인 후 상태 변경)를 제공하며, 로그인(JWT 발급)은 AuthController가 담당한다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /** 회원 가입. */
    @PostMapping
    public ApiResponse<UserJoinResponse> join(@Valid @RequestBody UserJoinRequest request) {
        return ApiResponse.success(userService.join(request));
    }

    /**
     * 회원 탈퇴. 인증 주체(로그인 ID)와 요청 본문의 비밀번호로 본인을 확인한 뒤
     * 탈퇴 상태로 변경한다. 실제 데이터 삭제는 익일 자정 배치가 수행한다.
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(Authentication authentication,
            @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(authentication.getName(), request);
        return ApiResponse.success(null);
    }
}