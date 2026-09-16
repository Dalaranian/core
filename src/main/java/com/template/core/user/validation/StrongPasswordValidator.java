package com.template.core.user.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.template.core.user.dto.UserJoinRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link StrongPassword} 제약의 검증기.
 *
 * <p>정책: 8~72자(72는 BCrypt 입력 제한), 영문 대/소문자·숫자·특수문자 중
 * 3종 이상 조합, 3회 이상 연속 문자(1234/abc, 역방향 포함) 및 반복 문자(1111) 금지,
 * 취약 사전 단어 금지, 로그인 ID 포함 금지.</p>
 *
 * <p>ponytail: 취약 비밀번호 판정은 소규모 블랙리스트 상수로 시작한다.
 * 계정 수가 늘어 정밀도가 필요해지면 외부 사전 파일(예: rockyou 기반 상위 N개) 로딩으로 교체.</p>
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, UserJoinRequest> {

	/** 비밀번호 최소 길이. 보안 강화 시 이 상수만 조정한다. */
	private static final int MIN_LENGTH = 8;
	/** BCrypt가 처리 가능한 최대 입력 길이. */
	private static final int MAX_LENGTH = 72;
	/** ID가 너무 짧으면(예: 2자) 포함 검사가 과도하게 제한되므로 하한을 둔다. */
	private static final int MIN_ID_LENGTH_FOR_CONTAINS_CHECK = 3;
	/** 흔히 사용되는 취약 비밀번호(및 포함 단어) 블랙리스트. */
	private static final Set<String> BLACKLIST = Set.of(
			"password", "passwd", "qwerty", "123456", "abc123", "admin", "letmein", "iloveyou", "123456789");

	@Override
	public boolean isValid(UserJoinRequest request, ConstraintValidatorContext context) {
		String pw = request.pw();
		// null/blank는 @NotBlank @Size가 담당하므로 여기서는 정책 위반으로 보지 않는다.
		if (pw == null || pw.isBlank()) {
			return true;
		}

		List<String> violations = new ArrayList<>();
		checkLength(pw, violations);
		checkComposition(pw, violations);
		checkSequencesAndRepeats(pw, violations);
		checkBlacklist(pw, violations);
		checkPersonalInfo(request.id(), pw, violations);

		if (violations.isEmpty()) {
			return true;
		}
		// 클래스 레벨 제약이지만 오류를 pw 필드에 매핑해 응답의 fieldErrors["pw"]로 내려간다.
		context.disableDefaultConstraintViolation();
		for (String violation : violations) {
			context.buildConstraintViolationWithTemplate(violation)
					.addPropertyNode("pw")
					.addConstraintViolation();
		}
		return false;
	}

	private void checkLength(String pw, List<String> violations) {
		if (pw.length() < MIN_LENGTH) {
			violations.add("비밀번호는 " + MIN_LENGTH + "자 이상이어야 합니다.");
		}
		if (pw.length() > MAX_LENGTH) {
			violations.add("비밀번호는 " + MAX_LENGTH + "자 이하여야 합니다.");
		}
	}

	private void checkComposition(String pw, List<String> violations) {
		boolean hasUpper = false;
		boolean hasLower = false;
		boolean hasDigit = false;
		boolean hasSpecial = false;
		for (int i = 0; i < pw.length(); i++) {
			char c = pw.charAt(i);
			if (Character.isUpperCase(c)) {
				hasUpper = true;
			} else if (Character.isLowerCase(c)) {
				hasLower = true;
			} else if (Character.isDigit(c)) {
				hasDigit = true;
			} else {
				hasSpecial = true;
			}
		}
		int kinds = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
		if (kinds < 3) {
			violations.add("비밀번호는 영문 대/소문자, 숫자, 특수문자 중 3종류 이상을 조합해야 합니다.");
		}
	}

	private void checkSequencesAndRepeats(String pw, List<String> violations) {
		for (int i = 0; i + 2 < pw.length(); i++) {
			char a = pw.charAt(i);
			char b = pw.charAt(i + 1);
			char c = pw.charAt(i + 2);
			if (Character.toLowerCase(a) == Character.toLowerCase(b)
					&& Character.toLowerCase(b) == Character.toLowerCase(c)) {
				violations.add("비밀번호에 같은 문자를 3회 이상 연속 사용할 수 없습니다.");
				return;
			}
			if ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1)) {
				violations.add("비밀번호에 연속된 문자(예: 123, abc)를 사용할 수 없습니다.");
				return;
			}
		}
	}

	private void checkBlacklist(String pw, List<String> violations) {
		String lower = pw.toLowerCase();
		for (String word : BLACKLIST) {
			if (lower.contains(word)) {
				violations.add("쉽게 추측할 수 있는 비밀번호는 사용할 수 없습니다.");
				return;
			}
		}
	}

	private void checkPersonalInfo(String id, String pw, List<String> violations) {
		if (id != null && id.length() >= MIN_ID_LENGTH_FOR_CONTAINS_CHECK
				&& pw.toLowerCase().contains(id.toLowerCase())) {
			violations.add("비밀번호에 로그인 ID를 포함할 수 없습니다.");
		}
	}
}