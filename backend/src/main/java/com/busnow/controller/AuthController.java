package com.busnow.controller;

import com.busnow.dto.auth.LoginRequest;
import com.busnow.dto.auth.RegisterRequest;
import com.busnow.dto.auth.TokenResponse;
import com.busnow.repository.UsersRepository;
import com.busnow.security.JwtTokenProvider;
import com.busnow.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 인증(Auth) REST 컨트롤러 (리팩토링 버전).
 * Base URL: /api/auth
 *
 * ✅ Cookie 처리 단순화:
 *    AuthService.login/refresh가 TokenResponse만 반환하고,
 *    Refresh Token은 UsersRepository를 통해 직접 조회하여 Cookie 설정.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsersRepository usersRepository;

    private final String RT_COOKIE_NAME = "busnow_rt_v2"; // ✅ 완전히 새로운 이름으로 변경하여 과거 좀비 쿠키 완벽 무시
    private final int RT_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7일(초)

    // ============================================================
    // POST /api/auth/register - 회원가입
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "회원가입이 완료되었습니다."));
    }

    // ============================================================
    // POST /api/auth/login - 로그인
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.login(request);
        
        // 혹시 남아있을지 모르는 과거 경로의 좀비 쿠키들을 선제적으로 제거
        expireGhostCookies(response);
        
        setRefreshTokenCookie(response, tokenResponse.refreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    private void expireGhostCookies(HttpServletResponse response) {
        ResponseCookie cookieApi = ResponseCookie.from(RT_COOKIE_NAME, "")
                .httpOnly(true).secure(false).path("/api").maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieApi.toString());

        ResponseCookie cookieAuth = ResponseCookie.from(RT_COOKIE_NAME, "")
                .httpOnly(true).secure(false).path("/api/auth").maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieAuth.toString());
    }

    // ============================================================
    // POST /api/auth/refresh - Access Token 재발급
    // ============================================================
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractCookie(request, RT_COOKIE_NAME);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            TokenResponse tokenResponse = authService.refresh(refreshToken);
            // ✅ 새 RT로 쿠키 즉시 갱신
            setRefreshTokenCookie(response, tokenResponse.refreshToken());
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.warn("[Auth] 토큰 재발급 실패 - 쿠키를 삭제합니다: {}", e.getMessage());
            // ✅ 실패 시 쿠키 강제 삭제 (무한 루프 방지)
            expireCookie(response, RT_COOKIE_NAME);
            throw e; // 예외를 그대로 던져서 GlobalExceptionHandler가 401/500 처리하게 함
        }
    }

    // ============================================================
    // POST /api/auth/signout - 로그아웃
    // ============================================================
    @PostMapping("/signout")
    public ResponseEntity<Map<String, String>> signout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Access Token에서 userId 추출 후 DB RT 삭제
        String accessToken = extractBearerToken(request);
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            authService.logout(jwtTokenProvider.getUserId(accessToken));
        }

        // Cookie 즉시 만료
        expireCookie(response, RT_COOKIE_NAME);
        return ResponseEntity.ok(Map.of("message", "로그아웃이 완료되었습니다."));
    }

    // ============================================================
    // 쿠키 유틸리티
    // ============================================================

    private void setRefreshTokenCookie(HttpServletResponse response, String value) {
        ResponseCookie cookie = ResponseCookie.from(RT_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(false)
                .path("/")             // ✅ 다시 /로 확장
                .maxAge(RT_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.info("[Auth] Set-Cookie 헤더 추가 완료 (Path=/)");
    }

    private void expireCookie(HttpServletResponse response, String name) {
        // Path=/ 쿠키 삭제
        ResponseCookie cookieRoot = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieRoot.toString());

        // Path=/api 쿠키 삭제 (과거 잔재)
        ResponseCookie cookieApi = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/api")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieApi.toString());

        // Path=/api/auth 쿠키 삭제 (과거 잔재)
        ResponseCookie cookieAuth = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookieAuth.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.warn("[Auth] 쿠키가 전혀 존재하지 않음");
            return null;
        }

        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .peek(c -> log.info("[Auth] 발견된 쿠키: name={}, path={}, valueStart={}, length={}", 
                        c.getName(), c.getPath(), 
                        c.getValue().length() > 5 ? c.getValue().substring(0, 5) : c.getValue(),
                        c.getValue().length()))
                .map(Cookie::getValue)
                .filter(val -> val != null && val.length() > 20)
                .map(val -> {
                    // Tomcat이나 특정 브라우저 환경에서 쿠키 값에 따옴표가 포함되어 전송되는 경우 대비
                    if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                        return val.substring(1, val.length() - 1);
                    }
                    return val;
                })
                .findFirst()
                .orElse(null);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer "))
                ? header.substring(7)
                : null;
    }
}
