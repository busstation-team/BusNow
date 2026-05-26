package com.busnow.config;

import com.busnow.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6.x 설정.
 *
 * ✅ Spring Boot 3.x (Security 6.x) 변경 사항:
 *    - WebSecurityConfigurerAdapter 제거 → @Bean SecurityFilterChain 방식 사용.
 *    - antMatchers → requestMatchers로 변경.
 *    - csrf/cors 설정: Lambda DSL 방식 사용.
 *    - STATELESS 세션: JWT 방식이므로 서버 세션 완전 비활성화.
 *
 * @EnableMethodSecurity: 컨트롤러 메서드에 @PreAuthorize("hasRole('ADMIN')") 등 사용 가능.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    // ============================================================
    // 공개(인증 불필요) 엔드포인트 정의
    // ============================================================
    // ✅ 인증 불필요 공개 엔드포인트
    private static final String[] PUBLIC_URLS = {
            "/auth/**",          // 로그인, 회원가입, 토큰 재발급
            "/stops/search",     // 정류소 검색 (비로그인 허용)
            "/bus/arrival/**",   // 버스 도착 정보 조회 (비로그인 허용)
    };

    // 인증 필요 엔드포인트 (SecurityConfig 참조용 주석):
    // /favorites/**    → 즐겨찾기 CRUD, 메인 대시보드 폴링
    // /notifications/**→ 알림 설정 CRUD + 토글
    // anyRequest().authenticated() 규칙으로 자동 보호됨

    // ============================================================
    // 핵심 보안 필터 체인
    // ============================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CSRF 비활성화: Stateless JWT 방식에서 CSRF 토큰 불필요
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ CORS 설정: corsConfigurationSource 빈에서 정의
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ 세션 완전 비활성화: JWT는 서버 세션 불필요 (STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ✅ 요청별 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight
                        .anyRequest().authenticated()                           // 나머지는 인증 필요
                )

                // ✅ 401 미인증, 403 미인가 처리
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(401);
                            response.getWriter().write(
                                    "{\"error\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(403);
                            response.getWriter().write(
                                    "{\"error\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\"}"
                            );
                        })
                )

                // ✅ JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 배치
                //    이렇게 해야 JWT 인증이 폼 로그인보다 먼저 실행됨
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ============================================================
    // 인증 공급자 (AuthenticationProvider)
    // ============================================================

    /**
     * DaoAuthenticationProvider:
     * UserDetailsService + PasswordEncoder를 사용하여
     * AuthenticationManager가 실제 인증 로직을 수행할 수 있게 연결.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager:
     * AuthService에서 직접 주입받아 로그인 인증에 사용.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // ============================================================
    // 비밀번호 인코더
    // ============================================================

    /**
     * BCryptPasswordEncoder (strength=12):
     * Spring Security 표준 단방향 해시 인코더.
     * 기본값(10)보다 strength를 12로 높여 보안 강화.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ============================================================
    // CORS 설정
    // ============================================================

    /**
     * React 개발 서버(localhost:5173)와 운영 도메인을 허용.
     * 프로덕션 배포 시 allowedOrigins를 실제 도메인으로 변경할 것.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ 개발: Vite 기본 포트 / 운영: 실제 도메인으로 교체
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",  // Vite 개발 서버
                "http://localhost:3000"   // Create React App 호환
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept"
        ));

        // ✅ Refresh Token HttpOnly Cookie 전송을 위해 반드시 true
        config.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (1시간)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
