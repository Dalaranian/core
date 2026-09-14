package com.template.core.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 탈퇴 회원 데이터를 주기적으로 삭제하는 배치.
 *
 * <p>매일 자정({@code user.withdrawal.delete-cron})에 실행되며,
 * 탈퇴 신청 후 {@code user.withdrawal.grace-days}일이 지난(기본 1일 = 요청일 익일 자정)
 * WITHDRAWN 상태 회원을 삭제한다.</p>
 */
@Component
@RequiredArgsConstructor
public class WithdrawalCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalCleanupScheduler.class);

    private final UserRepository userRepository;
    private final WithdrawalProperties properties;

    /** 익일 자정(또는 유예 기간 경과 시점)에 탈퇴 회원 데이터를 삭제한다. */
    @Scheduled(cron = "${user.withdrawal.delete-cron:0 0 0 * * *}")
    @Transactional
    public void purgeWithdrawnUsers() {
        // ponytail: 삭제 대상 판단 기준을 "현재 날짜 - 유예기간" 시작 시각으로 단순화했다.
        // 유예 1일이면 "어제 탈퇴 신청분"이 오늘 자정(=익일)에 삭제된다.
        LocalDateTime cutoff = LocalDate.now().minusDays(properties.graceDays() - 1).atStartOfDay();
        int deleted = userRepository.deleteWithdrawnBefore(cutoff);
        if (deleted > 0) {
            log.info("탈퇴 회원 데이터 삭제 완료. cutoff={}, deleted={}", cutoff, deleted);
        }
    }
}
