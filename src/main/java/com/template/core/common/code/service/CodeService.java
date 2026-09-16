package com.template.core.common.code.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.core.common.code.dto.CodeGroupResponse;
import com.template.core.common.code.dto.CodeTreeResponse;
import com.template.core.common.code.entity.CodeEntity;
import com.template.core.common.code.entity.CodeGroupEntity;
import com.template.core.common.code.repository.CodeGroupRepository;
import com.template.core.common.code.repository.CodeRepository;

/**
 * 공통 코드 조회/관리 서비스.
 *
 * <p>코드 테이블을 메모리 캐시에 올려두고 조회하며, 관리 API로 코드가
 * 변경되면 캐시를 무효화해 다음 조회 시 재적재한다.
 * 생성/수정은 createGroup/createCode/updateCode/disableCode 메서드로 제공한다.</p>
 */
@Service
public class CodeService {

    private final CodeGroupRepository codeGroupRepository;
    private final CodeRepository codeRepository;

    /**
     * ponytail: 코드 테이블 전체를 메모리에 적재하는 캐시. 공통 코드는 통상
     * 수십~수백 건 수준이라 이 방식이 충분하며, 수만 건 이상으로 커지면
     * 그룹 단위 지연 로딩으로 교체한다(업그레이드 경로).
     */
    private volatile Map<String, List<CodeEntity>> cache;

    public CodeService(CodeGroupRepository codeGroupRepository, CodeRepository codeRepository) {
        this.codeGroupRepository = codeGroupRepository;
        this.codeRepository = codeRepository;
    }

    /** 그룹+코드값으로 활성 코드를 조회한다. */
    public Optional<CodeEntity> getCode(String groupCode, String code) {
        return ensureLoaded().getOrDefault(groupCode, List.of()).stream()
                .filter(c -> c.isUseYn() && c.getCode().equals(code))
                .findFirst();
    }

    /** 특정 코드의 활성 자식 코드를 조회한다. parentCode가 null이면 그룹 1레벨. */
    public List<CodeEntity> getChildren(String groupCode, String parentCode) {
        return ensureLoaded().getOrDefault(groupCode, List.of()).stream()
                .filter(c -> c.isUseYn()
                        && (parentCode == null
                                ? c.getParentCode() == null
                                : parentCode.equals(c.getParentCode())))
                .sorted(CodeService::bySortOrder)
                .toList();
    }

    /** 그룹의 활성 코드 트리를 조회한다(비즈니스 조회용). */
    public List<CodeTreeResponse> getTree(String groupCode) {
        return buildTree(groupCode, true);
    }

    /** 그룹의 코드 트리를 조회한다. 관리 화면처럼 비활성 코드도 보려면 includeDisabled=true. */
    public List<CodeTreeResponse> getTree(String groupCode, boolean includeDisabled) {
        return buildTree(groupCode, !includeDisabled);
    }

    /** 전체 그룹과 각 그룹의 코드 트리를 조회한다(관리 화면용, 비활성 포함). */
    public List<CodeGroupResponse> getAllGroups() {
        return codeGroupRepository.findAllByOrderByGroupCodeAsc().stream()
                .map(group -> new CodeGroupResponse(group, buildTree(group.getGroupCode(), false)))
                .toList();
    }

    /** 새 코드 그룹을 생성한다. */
    @Transactional
    public CodeGroupEntity createGroup(com.template.core.common.code.dto.CodeGroupCreateRequest request) {
        if (request.groupCode() == null || request.groupCode().isBlank()) {
            throw new IllegalArgumentException("그룹 식별자(groupCode)는 필수입니다.");
        }
        codeGroupRepository.findByGroupCode(request.groupCode()).ifPresent(group -> {
            throw new IllegalStateException("이미 존재하는 코드 그룹입니다: " + request.groupCode());
        });
        return codeGroupRepository.save(CodeGroupEntity.builder()
                .groupCode(request.groupCode())
                .groupName(request.groupName() != null ? request.groupName() : request.groupCode())
                .description(request.description())
                .build());
    }

    /**
     * 새 코드를 생성한다. parentCode 지정 시 같은 그룹 내 상위 코드가
     * 존재해야 하며, 이를 통해 2레벨/3레벨 등 N계층을 구성한다.
     */
    @Transactional
    public CodeEntity createCode(String groupCode, com.template.core.common.code.dto.CodeCreateRequest request) {
        codeGroupRepository.findByGroupCode(groupCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코드 그룹입니다: " + groupCode));
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("코드값(code)은 필수입니다.");
        }
        if (codeRepository.existsByGroupCodeAndCode(groupCode, request.code())) {
            throw new IllegalStateException("이미 존재하는 코드입니다: " + groupCode + "/" + request.code());
        }
        if (request.parentCode() != null
                && !codeRepository.existsByGroupCodeAndCode(groupCode, request.parentCode())) {
            throw new IllegalArgumentException("상위 코드가 같은 그룹에 존재하지 않습니다: " + request.parentCode());
        }
        CodeEntity saved = codeRepository.save(CodeEntity.builder()
                .groupCode(groupCode)
                .code(request.code())
                .parentCode(request.parentCode())
                .name(request.name() != null ? request.name() : request.code())
                .description(request.description())
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build());
        invalidate();
        return saved;
    }

    /**
     * 코드의 표시 정보(name/description/sortOrder/useYn)를 수정한다.
     * enum 유래 시드 코드는 원본 enum이 싱크 원본이므로 수정할 수 없다.
     */
    @Transactional
    public void updateCode(String groupCode, String code,
            com.template.core.common.code.dto.CodeUpdateRequest request) {
        CodeEntity entity = codeRepository.findByGroupCodeAndCode(groupCode, code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코드입니다: " + groupCode + "/" + code));
        if (entity.isSeedYn()) {
            throw new IllegalArgumentException(
                    "enum에서 유래한 시드 코드는 수정할 수 없습니다: " + groupCode + "/" + code);
        }
        if (request.name() != null) {
            entity.updateName(request.name());
        }
        if (request.description() != null) {
            entity.updateDescription(request.description());
        }
        if (request.sortOrder() != null) {
            entity.updateSortOrder(request.sortOrder());
        }
        if (request.useYn() != null) {
            entity.updateUseYn(request.useYn());
        }
        invalidate();
    }

    /**
     * 코드를 비활성화한다(soft delete). 활성 자식 코드가 남아 있으면
     * 트리가 끊기므로 비활성화를 거부한다.
     */
    @Transactional
    public void disableCode(String groupCode, String code) {
        CodeEntity entity = codeRepository.findByGroupCodeAndCode(groupCode, code)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코드입니다: " + groupCode + "/" + code));
        if (entity.isSeedYn()) {
            throw new IllegalArgumentException(
                    "enum에서 유래한 시드 코드는 비활성화할 수 없습니다: " + groupCode + "/" + code);
        }
        if (codeRepository.existsByGroupCodeAndParentCodeAndUseYnTrue(groupCode, code)) {
            throw new IllegalStateException(
                    "활성 상태인 하위 코드가 있어 비활성화할 수 없습니다: " + groupCode + "/" + code);
        }
        entity.updateUseYn(false);
        invalidate();
    }

    /** 캐시를 무효화한다. 다음 조회 시 DB에서 재적재된다. */
    public void invalidate() {
        cache = null;
    }

    private Map<String, List<CodeEntity>> ensureLoaded() {
        Map<String, List<CodeEntity>> local = cache;
        if (local == null) {
            synchronized (this) {
                if (cache == null) {
                    Map<String, List<CodeEntity>> grouped = new HashMap<>();
                    for (CodeEntity code : codeRepository.findAll()) {
                        grouped.computeIfAbsent(code.getGroupCode(), k -> new ArrayList<>()).add(code);
                    }
                    cache = grouped;
                }
                local = cache;
            }
        }
        return local;
    }

    private List<CodeTreeResponse> buildTree(String groupCode, boolean enabledOnly) {
        List<CodeEntity> all = ensureLoaded().getOrDefault(groupCode, List.of());
        Map<String, List<CodeEntity>> byParent = new HashMap<>();
        for (CodeEntity code : all) {
            if (enabledOnly && !code.isUseYn()) {
                continue;
            }
            byParent.computeIfAbsent(code.getParentCode(), k -> new ArrayList<>()).add(code);
        }
        return buildNodes(null, byParent);
    }

    /** parent_code로 묶은 목록에서 1레벨부터 자식을 재귀적으로 조립해 N계층 트리를 만든다. */
    private List<CodeTreeResponse> buildNodes(String parentCode, Map<String, List<CodeEntity>> byParent) {
        List<CodeTreeResponse> nodes = new ArrayList<>();
        for (CodeEntity code : byParent.getOrDefault(parentCode, List.of())) {
            nodes.add(CodeTreeResponse.from(code, buildNodes(code.getCode(), byParent)));
        }
        nodes.sort((a, b) -> Integer.compare(
                a.sortOrder() == null ? 0 : a.sortOrder(),
                b.sortOrder() == null ? 0 : b.sortOrder()));
        return nodes;
    }

    private static int bySortOrder(CodeEntity a, CodeEntity b) {
        return Integer.compare(a.getSortOrder() == null ? 0 : a.getSortOrder(),
                b.getSortOrder() == null ? 0 : b.getSortOrder());
    }
}
