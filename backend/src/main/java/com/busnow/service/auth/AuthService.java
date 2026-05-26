package com.busnow.service.auth;

import com.busnow.dto.auth.LoginRequest;
import com.busnow.dto.auth.RegisterRequest;
import com.busnow.dto.auth.TokenResponse;
import com.busnow.entity.Users;
import com.busnow.repository.UsersRepository;
import com.busnow.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증(Auth) 비즈니스 로직 서비스.
 *
 * 회원가입, 로그인, 토큰 재발급, 로그아웃 처리.
 *
 * ✅ Refresh Token 로테이션 전략:
 *    재발급 요청 시 기존 RT를 무효화하고 새 RT를 발급 + DB 업데이트.
 *    이를 통해 RT 탈취 후 재사용 감지 가능 (이미 사용된 RT면 DB와 불일치).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // ============================================================
    // 회원가입
    // ============================================================

    /**
     * 신규 사용자 등록.
     *
     * @param request 회원가입 요청 DTO (username, password, email)
     * @throws IllegalArgumentException 아이디 또는 이메일 중복 시
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 중복 검증
        if (usersRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다: " + request.username());
        }
        if (usersRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.email());
        }

        // ✅ 비밀번호 BCrypt 해싱 후 저장 (평문 저장 절대 금지)
        Users newUser = Users.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role("USER")
                .build();

        usersRepository.save(newUser);
        log.info("[Auth] 회원가입 완료: {}", request.username());
    }

    // ============================================================
    // 로그인
    // ============================================================

    /**
     * 로그인 처리 및 Access/Refresh Token 발급.
     *
     * @param request 로그인 요청 DTO (username, password)
     * @return TokenResponse (accessToken 포함, refreshToken은 반환값에 미포함)
     * @throws org.springframework.security.core.AuthenticationException 인증 실패 시
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        log.info("[Auth] 로그인 시도: {}", request.username());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        Users user = usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("인증 후 사용자 조회 실패"));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        log.info("[Auth] 토큰 생성 완료 - 저장할 RT: length={}, start={}", 
                refreshToken.length(), 
                refreshToken.length() > 15 ? refreshToken.substring(0, 15) : refreshToken);

        // ✅ 명시적 쿼리로 확실하게 업데이트
        usersRepository.updateRefreshToken(user.getUserId(), refreshToken);
        usersRepository.flush(); // 즉시 DB 반영
        log.info("[Auth] DB Refresh Token 업데이트 완료");

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(),
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        log.info("[Auth] 재발급 시도 - 수신한 RT: length={}, start={}", 
                refreshToken.length(), 
                refreshToken.length() > 15 ? refreshToken.substring(0, 15) : refreshToken);

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("[Auth] 유효하지 않거나 만료된 Refresh Token");
            throw new RuntimeException("REFRESH_TOKEN_EXPIRED");
        }

        Users user = usersRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("[Auth] DB에 없는 Refresh Token이 전달됨 (탈취 또는 중복 쿠키 의심)");
                    return new RuntimeException("REFRESH_TOKEN_NOT_FOUND");
                });

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        usersRepository.updateRefreshToken(user.getUserId(), newRefreshToken);
        usersRepository.flush();

        log.info("[Auth] 토큰 재발급 완료: {}", user.getUsername());

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiration(),
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Transactional
    public void logout(Integer userId) {
        usersRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            usersRepository.save(user);
            log.info("[Auth] 로그아웃 처리 완료 (토큰 삭제): userId={}", userId);
        });
    }

}
