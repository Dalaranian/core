package com.template.core.user.dto;

import com.template.core.user.entity.UserEntity;

/**
 * 로그인 응답 DTO.
 *
 * @param accessToken 발급된 JWT
 * @param seq         사용자 순번(PK)
 * @param id          로그인 ID
 * @param userName    사용자 이름
 */
public record LoginResponse(String accessToken, Long seq, String id, String userName) {

    /** JWT와 사용자 엔티티로부터 응답 DTO를 생성한다. */
    public static LoginResponse of(String accessToken, UserEntity user) {
        return new LoginResponse(accessToken, user.getSeq(), user.getId(), user.getUserName());
    }
}
