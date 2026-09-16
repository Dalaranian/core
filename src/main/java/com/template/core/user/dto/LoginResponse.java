package com.template.core.user.dto;

import com.template.core.user.entity.UserEntity;

/**
 * 로그인 응답 DTO.
 *
 * @param accessToken 발급된 JWT
 * @param id        로그인 ID
 * @param userName  사용자 이름
 */
public record LoginResponse(String accessToken, String id, String userName) {

    /** JWT와 사용자 엔티티로부터 응답 DTO를 생성한다. */
    public static LoginResponse of(String accessToken, UserEntity user) {
        return new LoginResponse(accessToken, user.getId(), user.getUserName());
    }
}
