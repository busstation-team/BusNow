package com.busnow.dto.auth;

/**
 * JWT 토큰 응답 DTO.
 *
 * ✅ Refresh Token 전달 전략:
 *    - accessToken: 응답 바디로 전달 → 프론트엔드 메모리(useState)에만 저장.
 *    - refreshToken: HttpOnly Cookie로 Set-Cookie 헤더에 설정 (AuthController에서 처리).
 *      이 DTO에는 RT를 포함시키지 않음으로써 XSS 공격으로 JS에서 RT 탈취 불가.
 *
 * @param accessToken  JWT Access Token (Bearer 접두사 미포함, 순수 토큰 값)
 * @param tokenType    토큰 타입 (항상 "Bearer")
 * @param expiresIn    Access Token 만료 시간 (밀리초)
 * @param userId       인증된 사용자 ID
 * @param username     인증된 사용자 아이디
 * @param role         사용자 권한 (USER / ADMIN)
 */
import com.fasterxml.jackson.annotation.JsonIgnore;

public record TokenResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        Integer userId,
        String username,
        String role,

        @JsonIgnore
        String refreshToken  // ✅ 내부 전달용 (응답 바디에는 포함 안됨)
) {
    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn,
                                   Integer userId, String username, String role) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, userId, username, role, refreshToken);
    }
}
