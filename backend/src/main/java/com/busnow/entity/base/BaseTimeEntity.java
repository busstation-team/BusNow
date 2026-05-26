package com.busnow.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 공통 감사(Audit) 필드를 제공하는 추상 베이스 엔티티.
 * DB 컬럼명: created_at, updated_at (소문자 snake_case)
 *
 * @EntityListeners(AuditingEntityListener.class): Spring Data JPA의 Auditing 기능 활성화.
 * @MappedSuperclass: 이 클래스 자체는 테이블과 매핑되지 않고, 상속받는 엔티티의 테이블에 컬럼이 추가됨.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /**
     * 레코드 최초 생성 시각.
     * updatable = false: 한 번 설정된 값은 UPDATE 쿼리에서 제외.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 레코드 마지막 수정 시각.
     * INSERT/UPDATE 시 자동으로 현재 시각으로 갱신.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
