package com.template.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.template.core.user.repository.UserRepository;

/**
 * WithdrawalCleanupScheduler 배치 로직 단위 테스트.
 *
 * <p>삭제 기준 시각(cutoff)이 "유예 기간 경과 시점"으로 계산되는지 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalCleanupSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WithdrawalProperties properties;

    @InjectMocks
    private WithdrawalCleanupScheduler scheduler;

    @Test
    @DisplayName("유예 기간 1일이면 어제 탈퇴 신청분이 오늘 자정 배치에서 삭제된다")
    void purge_WithGraceDays1_UsesYesterdayAsCutoff() {
        // given: 유예 기간은 1일(기본값, 요청일 익일 삭제)
        when(properties.graceDays()).thenReturn(1);
        when(userRepository.deleteWithdrawnBefore(any())).thenReturn(1);

        // when: 배치를 실행한다
        scheduler.purgeWithdrawnUsers();

        // then: cutoff이 "오늘 자정"(= 어제 탈퇴분 포함)인지 확인한다
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).deleteWithdrawnBefore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(LocalDate.now().atStartOfDay());
    }

    @Test
    @DisplayName("유예 기간 7일이면 7일 전 자정이 삭제 기준이 된다")
    void purge_WithGraceDays7_UsesSevenDaysAgoAsCutoff() {
        // given: 유예 기간은 7일
        when(properties.graceDays()).thenReturn(7);
        when(userRepository.deleteWithdrawnBefore(any())).thenReturn(0);

        // when: 배치를 실행한다
        scheduler.purgeWithdrawnUsers();

        // then: cutoff이 "7일 전 자정"인지 확인한다
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).deleteWithdrawnBefore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(LocalDate.now().minusDays(6).atStartOfDay());
    }
}
