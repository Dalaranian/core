package com.template.core.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 회원 탈퇴 관련 설정값 바인딩 객체. application.yaml의 {@code user.withdrawal.*} 를 묶어 주입받는다.
 *
 * @param graceDays  탈퇴 신청 후 삭제까지의 유예 일수. 1이면 "요청일 익일 자정"에 삭제된다.
 * @param deleteCron 삭제 배치 실행 cron 표현식 (기본: 매일 자정)
 */
@ConfigurationProperties(prefix = "user.withdrawal")
public record WithdrawalProperties(int graceDays, String deleteCron) {

    public WithdrawalProperties {
        if (graceDays < 1) {
            graceDays = 1;
        }
        if (deleteCron == null || deleteCron.isBlank()) {
            deleteCron = "0 0 0 * * *";
        }
    }
}
