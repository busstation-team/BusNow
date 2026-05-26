package com.busnow.repository;

import com.busnow.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Users 테이블 JPA Repository.
 *
 * ✅ 컬럼명 명시 쿼리:
 *    PhysicalNamingStrategyStandardImpl 사용으로 JPQL에서도
 *    DB 컬럼명이 아닌 Java 필드명(username, refreshToken)을 사용.
 *    단, @Query(nativeQuery=true) 사용 시 DB 컬럼명(Username, Refresh_token) 사용.
 */
public interface UsersRepository extends JpaRepository<Users, Integer> {

    /**
     * 로그인 아이디(Username)로 사용자 조회.
     * Spring Security UserDetailsService에서 사용.
     */
    Optional<Users> findByUsername(String username);

    /**
     * 이메일 중복 여부 확인.
     * 회원가입 검증에서 사용.
     */
    boolean existsByEmail(String email);

    /**
     * 아이디 중복 여부 확인.
     */
    boolean existsByUsername(String username);

    /**
     * Refresh Token으로 사용자 조회.
     * 토큰 재발급 요청 시 DB에 저장된 RT와 비교 검증.
     */
    Optional<Users> findByRefreshToken(String refreshToken);

    /**
     * Refresh Token 업데이트 (벌크 업데이트 - 더티 체킹 없이 직접 UPDATE).
     * @Modifying + @Transactional 조합 필수.
     *
     * ✅ JPQL 사용: refreshToken은 Java 필드명, User_id는 userId 필드명으로 참조.
     */
    @Modifying
    @Query("UPDATE Users u SET u.refreshToken = :refreshToken WHERE u.userId = :userId")
    void updateRefreshToken(@Param("userId") Integer userId,
                            @Param("refreshToken") String refreshToken);

    /**
     * 로그아웃 시 Refresh Token 삭제 (NULL로 설정).
     */
    @Modifying
    @Query("UPDATE Users u SET u.refreshToken = NULL WHERE u.userId = :userId")
    void deleteRefreshToken(@Param("userId") Integer userId);
}
