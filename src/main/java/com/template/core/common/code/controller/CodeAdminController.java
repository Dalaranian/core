package com.template.core.common.code.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.template.core.common.code.dto.CodeCreateRequest;
import com.template.core.common.code.dto.CodeGroupCreateRequest;
import com.template.core.common.code.dto.CodeGroupResponse;
import com.template.core.common.code.dto.CodeUpdateRequest;
import com.template.core.common.code.service.CodeService;
import com.template.core.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 공통 코드 관리 API. /admin/** 경로이므로 ROLE_ADMIN 권한이 필요하다.
 *
 * <p>enum 유래 시드 코드(seedYn=true)는 읽기만 가능하고,
 * 동적 코드는 생성/수정/비활성화가 가능하다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/codes")
public class CodeAdminController {

    private final CodeService codeService;

    /** 전체 그룹과 각 그룹의 코드 트리(비활성 포함)를 조회한다. */
    @GetMapping
    public ApiResponse<List<CodeGroupResponse>> groups() {
        return ApiResponse.success(codeService.getAllGroups());
    }

    /** 새 코드 그룹을 생성한다. */
    @PostMapping("/groups")
    public ApiResponse<Void> createGroup(@RequestBody CodeGroupCreateRequest request) {
        codeService.createGroup(request);
        return ApiResponse.success(null);
    }

    /** 그룹에 새 코드를 생성한다. parentCode 지정으로 2레벨/3레벨 등 N계층을 구성한다. */
    @PostMapping("/{groupCode}/codes")
    public ApiResponse<Void> createCode(@PathVariable String groupCode,
            @RequestBody CodeCreateRequest request) {
        codeService.createCode(groupCode, request);
        return ApiResponse.success(null);
    }

    /** 코드의 표시 정보를 수정한다(시드 코드는 수정 불가). */
    @PatchMapping("/{groupCode}/{code}")
    public ApiResponse<Void> updateCode(@PathVariable String groupCode, @PathVariable String code,
            @RequestBody CodeUpdateRequest request) {
        codeService.updateCode(groupCode, code, request);
        return ApiResponse.success(null);
    }

    /** 코드를 비활성화한다(soft delete, 시드 코드는 비활성화 불가). */
    @DeleteMapping("/{groupCode}/{code}")
    public ApiResponse<Void> disableCode(@PathVariable String groupCode, @PathVariable String code) {
        codeService.disableCode(groupCode, code);
        return ApiResponse.success(null);
    }
}
