package com.template.core.common.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.common.code.entity.CodeEntity;
import com.template.core.common.code.entity.CodeGroupEntity;
import com.template.core.common.code.repository.CodeGroupRepository;
import com.template.core.common.code.repository.CodeRepository;
import com.template.core.user.code.UserRole;
import com.template.core.user.code.UserStatus;

/**
 * 기동 시 enum 코드를 코드 테이블로 싱크하는 러너(단방향: enum → DB).
 *
 * <p>enum이 코드 정의의 원본이므로 시드 코드는 enum 값에 맞춰 upsert된다.
 * DB에 이미 같은 코드가 있는데 시드가 아니면(관리 API로 만든 동적 코드와
 * 코드값 충돌) 덮어쓰지 않고 경고 로그만 남긴다.</p>
 *
 * <p>계층 시딩: {@link CodeEnum#getParentCode()}를 오버라이드한 enum 값은
 * 상위 코드가 먼저 반영된 후 저장된다. 목록에는 상위 코드 상수를 먼저
 * 선언해 두는 것을 권장하며, 순서가 어긋나면 러너가 반복 시도로 해소한다.</p>
 */
@Component
public class CodeSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CodeSyncRunner.class);

    /**
     * DB로 싱크할 비즈니스 enum 값 목록.
     * ponytail: 새 코드 enum 추가 시 여기에 상수를 한 줄씩 등록한다.
     * 값이 수십 개를 넘어 관리가 부담스러워지면 클래스패스 스캔으로 교체한다.
     */
    private static final List<CodeEnum> SEED_CODES = List.of(
            UserStatus.ACTIVE, UserStatus.WITHDRAWN,
            UserRole.ROLE_USER, UserRole.ROLE_ADMIN);

    private final CodeGroupRepository codeGroupRepository;
    private final CodeRepository codeRepository;

    public CodeSyncRunner(CodeGroupRepository codeGroupRepository, CodeRepository codeRepository) {
        this.codeGroupRepository = codeGroupRepository;
        this.codeRepository = codeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 상위 코드가 아직 없는 값은 다음 패스에서 다시 시도한다(선언 순서 무관 보장).
        List<CodeEnum> pending = new ArrayList<>(SEED_CODES);
        while (!pending.isEmpty()) {
            boolean progressed = false;
            Iterator<CodeEnum> iterator = pending.iterator();
            while (iterator.hasNext()) {
                CodeEnum source = iterator.next();
                String parentCode = source.getParentCode();
                if (parentCode != null
                        && !codeRepository.existsByGroupCodeAndCode(source.getGroupCode(), parentCode)) {
                    continue;
                }
                syncGroup(source);
                syncCode(source);
                iterator.remove();
                progressed = true;
            }
            if (!progressed) {
                throw new IllegalStateException(
                        "enum 코드의 parentCode가 존재하지 않거나 순환 참조합니다: " + pending);
            }
        }
    }

    /** 그룹이 없으면 시드 그룹으로 생성한다. */
    private void syncGroup(CodeEnum source) {
        String groupCode = source.getGroupCode();
        codeGroupRepository.findByGroupCode(groupCode).ifPresentOrElse(group -> {
            if (!group.isSeedYn()) {
                log.warn("enum 그룹과 같은 식별자의 비시드 그룹이 이미 존재합니다. 그룹 정보는 유지됩니다: {}", groupCode);
            }
        }, () -> codeGroupRepository.save(CodeGroupEntity.builder()
                .groupCode(groupCode)
                .groupName(source.getGroupName())
                .seedYn(true)
                .build()));
    }

    /** 코드가 없으면 시드 코드로 생성하고, 시드 코드면 원본 enum 값에 맞춰 갱신한다. */
    private void syncCode(CodeEnum source) {
        String groupCode = source.getGroupCode();
        String codeValue = source.getCodeValue();
        codeRepository.findByGroupCodeAndCode(groupCode, codeValue).ifPresentOrElse(existing -> {
            if (!existing.isSeedYn()) {
                log.warn("enum 코드와 같은 코드값의 비시드 코드가 이미 존재합니다. 기존 데이터를 유지합니다: {}/{}",
                        groupCode, codeValue);
                return;
            }
            if (!existing.getName().equals(source.getName())
                    || !java.util.Objects.equals(existing.getDescription(), source.getDescription())
                    || !java.util.Objects.equals(existing.getSortOrder(), source.getSortOrder())) {
                log.info("시드 코드를 원본 enum 값으로 갱신합니다: {}/{}", groupCode, codeValue);
                existing.syncFrom(source);
            }
        }, () -> codeRepository.save(CodeEntity.seed(groupCode, source)));
    }
}
