package com.template.core.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.template.core.common.response.ApiResponse;

/**
 * GlobalExceptionHandler의 예외 → 상태 코드/응답 포맷 매핑을 검증한다.
 */
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("Bean Validation 실패는 400과 VALIDATION_ERROR, 필드별 오류로 변환된다")
    void validationFailure_Returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType("application/json")
                        .content("{\"id\": \"\", \"pw\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors.id").exists())
                .andExpect(jsonPath("$.error.fieldErrors.pw").exists());
    }

    @Test
    @DisplayName("IllegalStateException은 409와 공통 에러 봉투로 변환된다")
    void illegalState_Returns409() throws Exception {
        mockMvc.perform(get("/test/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_STATE"))
                .andExpect(jsonPath("$.error.message").value("이미 사용 중인 로그인 ID입니다."));
    }

    @Test
    @DisplayName("IllegalArgumentException은 400으로 변환된다")
    void illegalArgument_Returns400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("알 수 없는 예외는 500과 범용 메시지로 변환된다")
    void unexpected_Returns500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버에 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    @DisplayName("성공 응답은 success=true와 data에 DTO를 담는다")
    void success_WrapsData() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("alice"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /** 예외를 던지거나 성공 응답을 반환하는 테스트 전용 컨트롤러. */
    @RestController
    static class ThrowingController {
        @PostMapping("/test/valid")
        public String valid(@Valid @RequestBody ValidationTarget body) {
            return "ok";
        }

        @GetMapping("/test/illegal-state")
        public String illegalState() {
            throw new IllegalStateException("이미 사용 중인 로그인 ID입니다.");
        }

        @GetMapping("/test/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        @GetMapping("/test/unexpected")
        public String unexpected() {
            throw new RuntimeException("내부 정보 유출 가능 메시지");
        }

        @GetMapping("/test/success")
        public ApiResponse<SampleDto> success() {
            return ApiResponse.success(new SampleDto("alice"));
        }
    }

    record SampleDto(String id) {
    }

    /** Bean Validation 실패를 유발하는 테스트 전용 요청 본문. */
    record ValidationTarget(
            @NotBlank @Size(min = 8) String id,
            @NotBlank String pw) {
    }
}
