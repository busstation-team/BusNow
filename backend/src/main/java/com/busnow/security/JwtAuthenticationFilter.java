package com.busnow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터.
 *
 * ✅ OncePerRequestFilter: 동일 요청 내에서 필터가 한 번만 실행됨을 보장.
 *    forward/include 등 내부 디스패치 시 중복 실행 방지.
 *
 * ✅ 동작 흐름:
 *    1. Authorization 헤더에서 "Bearer {token}" 추출
 *    2. JwtTokenProvider로 토큰 유효성 검증
 *    3. 유효하면 username 추출 → DB에서 UserDetails 로드
 *    4. UsernamePasswordAuthenticationToken 생성 → SecurityContext에 저장
 *    5. 이후 컨트롤러에서 @AuthenticationPrincipal 또는 SecurityContextHolder로 인증 정보 접근 가능
 *
 * ✅ 인증 실패 시:
 *    SecurityContext에 아무것도 저장하지 않음.
 *    이후 SecurityConfig의 exceptionHandling이 401 응답 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            authenticateUser(token, request);
        }

        // 토큰이 없거나 유효하지 않으면 SecurityContext를 비운 채로 다음 필터로 이동.
        // 이후 접근 제어는 SecurityConfig의 authorizeHttpRequests가 담당.
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출.
     * "Bearer eyJhbG..." → "eyJhbG..."
     *
     * @return 순수 JWT 문자열 (없으면 null)
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 토큰에서 사용자 정보를 추출하여 SecurityContext에 인증 객체 저장.
     *
     * @param token   검증 완료된 JWT
     * @param request 현재 HTTP 요청 (IP, 세션 정보 등 details에 저장)
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        try {
            String username = jwtTokenProvider.getUsername(token);

            // SecurityContext에 인증 정보가 없을 때만 설정 (중복 DB 조회 방지)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                         // credentials: JWT 방식에서는 null
                                userDetails.getAuthorities()  // 권한 목록 (ROLE_USER 등)
                        );

                // 요청의 IP, 세션 ID 등 부가 정보를 details에 저장
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 완료 상태 저장
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("[JWT] 인증 성공 - 사용자: {}", username);
            }
        } catch (Exception e) {
            // DB 조회 실패 등 예외 발생 시 SecurityContext를 비운 채로 진행
            log.error("[JWT] SecurityContext 설정 중 오류: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}
