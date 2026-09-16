package com.template.core.common.code;

import java.util.Locale;

/**
 * 코드성 enum이 공통 코드 테이블(codes/code_groups)과 싱크를 맞추기 위한 공통 계약.
 *
 * <p>비즈니스 로직에 필요한 코드는 enum으로 타입 세이프하게 정의하고,
 * {@link CodeSyncRunner}가 애플리케이션 기동 시 이 인터페이스의 값을
 * 코드 테이블로 upsert(단방향 싱크)한다. DB에는 여기서 정의한 것 외에
 * 관리 API로 추가한 동적 코드도 저장될 수 있다(단, enum이 없으므로
 * 비즈니스 분기(== 비교)에는 사용할 수 없다).</p>
 *
 * <p>계층 구조: {@link #getParentCode()}를 오버라이드하면 enum 값 자체도
 * 2레벨 이하로 시딩할 수 있다. 이때 상위 코드 상수가 목록에서 먼저 선언되어야 한다.</p>
 */
public interface CodeEnum {

    /** DB 저장용 코드값(enum 코드 컨버터가 사용하는 값과 동일) */
    int getCode();

    /** 코드 설명. 코드 테이블의 description과 싱크된다. */
    String getDescription();

    /** 코드 테이블 저장용 문자열 코드값. int 코드는 "10" 형태로 문자열화된다. */
    default String getCodeValue() {
        return String.valueOf(getCode());
    }

    /** 코드명. 코드 테이블의 name과 싱크된다(기본값: enum 상수 이름). */
    default String getName() {
        return ((Enum<?>) this).name();
    }

    /** 상위 코드값. 그룹 1레벨 코드면 null. 2레벨 이상 시딩 시 오버라이드한다. */
    default String getParentCode() {
        return null;
    }

    /**
     * 이 enum이 속한 코드 그룹 식별자.
     * enum 클래스 이름을 SNAKE_CASE로 자동 변환한다(예: UserStatus → USER_STATUS).
     */
    default String getGroupCode() {
        String className = ((Enum<?>) this).getDeclaringClass().getSimpleName();
        return className.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    /** 코드 그룹의 표시 이름. 기본값은 그룹 식별자 그대로. */
    default String getGroupName() {
        return getGroupCode();
    }

    /** 코드 정렬 순서. 기본값은 enum 선언 순서(ordinal). */
    default int getSortOrder() {
        return ((Enum<?>) this).ordinal();
    }
}
