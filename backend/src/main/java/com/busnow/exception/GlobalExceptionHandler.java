package com.busnow.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러.
 *
 * @RestControllerAdvice: @ControllerAdvice + @ResponseBody 조합.
 * 모든 @RestController에서 발생하는 예외를 일관된 JSON 형식으로 변환.
 *
 *  응답 형식:
 *    {
 *      "timestamp": "2024-01-01T00:00:00",
 *      "status": 400,
 *      "error": "BAD_REQUEST",
 *      "message": "설명",
 *      "details": { ... }  // 옵션
 *    }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // 입력 유효성 검증 실패 (@Valid)
    // ============================================================

    /**
     * Bean Validation 실패 시 (예: @NotBlank, @Email 위반).
     * 모든 필드 오류를 Map으로 반환.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
            log.warn("[Validation Failed] Field: {}, Message: {}", error.getField(), error.getDefaultMessage());
        }

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "입력값이 올바르지 않습니다.",
                Map.of("fieldErrors", fieldErrors)
        );
    }

    // ============================================================
    // 인증 실패
    // ============================================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.", null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "비활성화된 계정입니다.", null);
    }

    // ============================================================
    // 비즈니스 로직 오류
    // ============================================================

    /**
     * 중복 회원가입, 유효하지 않은 RT 등 비즈니스 규칙 위반.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        // 토큰 관련 에러인 경우 401로 변경하여 반환
        if (ex.getMessage().contains("Refresh Token") || ex.getMessage().contains("토큰")) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.error("[Exception] IllegalState: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), null);
    }

    // ============================================================
    // 서버 내부 오류 (최후 방어선)
    // ============================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("[Exception] Unhandled exception: {}", ex.getMessage(), ex);
        // 디버깅을 위해 에러 메시지를 응답에 포함 (추후 제거 필요)
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 오류: " + ex.getMessage(),
                null
        );
    }

    // ============================================================
    // 공통 응답 빌더
    // ============================================================

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String message,
            Map<String, Object> details
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.name());
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
