package com.busnow.security;

import com.busnow.entity.Users;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Access/Refresh Token 생성·검증·파싱 유틸리티.
 *
 * ✅ jjwt 0.12.x API 준수:
 *    - Jwts.builder() → signWith(key) 사용 (알고리즘 자동 선택 HS256).
 *    - Jwts.parser() (0.12.x에서 parserBuilder() 대체).
 *    - SecretKey: Keys.hmacShaKeyFor(bytes) → HS256 최소 256비트(32바이트) 필요.
 *
 * ✅ Claims 설계:
 *    - sub(Subject): username (식별자)
 *    - userId: Users.User_id (DB PK, 서비스 계층에서 바로 사용)
 *    - role: Users.Role (인가 처리용)
 *    - type: "access" | "refresh" (토큰 유형 구분)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        // Base64 인코딩 없이 원문 바이트 사용 (application.yml의 secret 문자열이 충분히 길어야 함)
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // ============================================================
    // 토큰 생성
    // ============================================================

    /**
     * Access Token 생성.
     * 짧은 유효기간(30분), 인가 정보(role) 포함.
     *
     * @param user 인증된 사용자 엔티티
     * @return 서명된 JWT 문자열
     */
    public String generateAccessToken(Users user) {
        return buildToken(user, accessTokenExpiration, "access");
    }

    /**
     * Refresh Token 생성.
     * 긴 유효기간(7일), 최소한의 클레임만 포함 (보안 최소화).
     *
     * @param user 인증된 사용자 엔티티
     * @return 서명된 JWT 문자열
     */
    public String generateRefreshToken(Users user) {
        return buildToken(user, refreshTokenExpiration, "refresh");
    }

    /**
     * 공통 토큰 빌더.
     */
    private String buildToken(Users user, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getUsername())                    // sub: 로그인 아이디
                .claim("userId", user.getUserId())              // 커스텀 클레임: DB PK
                .claim("role", user.getRole())                  // 커스텀 클레임: 권한
                .claim("type", tokenType)                       // 커스텀 클레임: 토큰 유형
                .issuedAt(now)                                  // iat: 발급 시각
                .expiration(expiryDate)                         // exp: 만료 시각
                .signWith(secretKey)                            // HS256 서명
                .compact();
    }

    // ============================================================
    // 토큰 검증
    // ============================================================

    /**
     * 토큰 유효성 검증.
     * 서명 위변조, 만료, 형식 오류를 모두 검사.
     *
     * @param token 검증할 JWT 문자열
     * @return true: 유효한 토큰 / false: 유효하지 않은 토큰
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token); // 파싱 성공 = 유효
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] 만료된 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] 지원하지 않는 토큰 형식: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JWT] 잘못된 형식의 토큰: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("[JWT] 서명 검증 실패: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] 빈 토큰 문자열: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Refresh Token 전용 검증 (만료 여부 + type 클레임 확인).
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Refresh Token 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // ============================================================
    // 클레임 추출
    // ============================================================

    /**
     * 토큰에서 사용자 아이디(username) 추출.
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 DB PK(userId) 추출.
     */
    public Integer getUserId(String token) {
        return getClaims(token).get("userId", Integer.class);
    }

    /**
     * 토큰에서 권한(role) 추출.
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Access Token 만료 시간(밀리초) 반환.
     * TokenResponse.expiresIn 필드에 사용.
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    // ============================================================
    // 내부 유틸리티
    // ============================================================

    /**
     * JWT 파싱 및 Claims 반환.
     * 만료/위변조 시 JwtException 계열 예외 발생.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
