package com.busnow.entity;

import com.busnow.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * [테이블: Users]
 * 사용자 계정 정보 엔티티.
 *
 * ✅ PhysicalNamingStrategyStandardImpl 사용으로 @Column(name = ...)이 필수.
 *    자동 변환 없이 DB 컬럼명과 정확히 일치해야 함.
 */
@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users extends BaseTimeEntity {

    /**
     * PK. AUTO_INCREMENT.
     * @GeneratedValue: DB에서 자동 생성하므로 IDENTITY 전략 사용.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "User_id")
    private Integer userId;

    /**
     * 사용자 로그인 아이디. UNIQUE NOT NULL.
     */
    @Column(name = "Username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * BCrypt 암호화된 비밀번호.
     */
    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    /**
     * 이메일 주소. UNIQUE NOT NULL.
     */
    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 사용자 권한. 기본값: 'USER', 관리자: 'ADMIN'.
     */
    @Column(name = "Role", length = 20)
    @Builder.Default
    private String role = "USER";

    /**
     * JWT Refresh Token 저장 필드.
     * HttpOnly Cookie 방식을 권장하지만, DB 저장으로 토큰 무효화 기능 지원.
     * @Lob 대신 TEXT 타입 직접 지정.
     */
    @Column(name = "Refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
}
