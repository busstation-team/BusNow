package com.busnow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정.
 *
 * @EnableJpaAuditing: BaseTimeEntity의 @CreatedDate, @LastModifiedDate 어노테이션이
 * 동작하려면 이 설정이 반드시 필요.
 * SecurityConfig와 분리하여 테스트 슬라이싱(@DataJpaTest) 시 충돌 방지.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // JPA Auditing 활성화 목적으로만 존재하는 설정 클래스.
    // 추후 AuditorAware 빈 등록 시 이 클래스에 추가할 것.
}
