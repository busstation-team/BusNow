package com.busnow.security;

import com.busnow.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Security의 UserDetailsService 구현체.
 *
 * DB의 Users 테이블에서 Username 컬럼으로 사용자를 조회하여
 * Spring Security가 인식하는 UserDetails 객체로 변환.
 *
 * ✅ @Transactional(readOnly = true):
 *    SELECT 전용 트랜잭션으로 성능 최적화 (flush 스킵, 더티 체킹 비활성화).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    /**
     * 로그인 아이디(username)로 사용자 조회.
     *
     * @param username Users.Username 컬럼 값
     * @return Spring Security UserDetails (권한 포함)
     * @throws UsernameNotFoundException 해당 아이디의 사용자가 없을 때
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.busnow.entity.Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "사용자를 찾을 수 없습니다: " + username
                ));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())   // BCrypt 해시된 비밀번호
                .authorities(List.of(
                        // Spring Security 권한명은 "ROLE_" 접두사 필요
                        new SimpleGrantedAuthority("ROLE_" + user.getRole())
                ))
                .build();
    }
}
