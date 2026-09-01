package com.template.core.user.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.template.core.user.service.UserService;
import com.template.core.user.dto.UserJoinRequest;
import com.template.core.user.dto.UserJoinResponse;

import lombok.RequiredArgsConstructor;

/**
 * 회원 관리용 컨트롤러.
 *
 * <p>현재는 회원 가입만 제공하며, 로그인(JWT 발급)은 다음 단계에서 추가한다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /** 회원 가입. */
    @PostMapping
    public UserJoinResponse join(@RequestBody UserJoinRequest request) {
        return userService.join(request);
    }
}