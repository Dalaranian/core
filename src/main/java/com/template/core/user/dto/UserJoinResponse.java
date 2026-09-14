package com.template.core.user.dto;

import com.template.core.user.entity.UserEntity;

/**
 * 회원 가입 응답 DTO.
 *
 * @param seq      사용자 순번(PK)
 * @param id       로그인 ID
 * @param userName 사용자 이름
 */
public record UserJoinResponse(Long seq, String id, String userName) {

    /** {@link UserEntity}로부터 응답 DTO를 생성한다. */
    public static UserJoinResponse from(UserEntity user) {
        return new UserJoinResponse(user.getSeq(), user.getId(), user.getUserName());
    }
}