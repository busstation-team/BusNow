package com.busnow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * [테이블: Favorite_Stops]
 * 사용자별 즐겨찾기 정류장 엔티티.
 *
 * ✅ FetchType.LAZY 강제: N+1 쿼리 방지.
 *    즐겨찾기 목록 조회 시 Users/Stops 데이터가 필요한 경우
 *    반드시 JOIN FETCH 또는 @EntityGraph를 활용할 것.
 *
 * - Created_at만 존재하는 단방향 감사 필드 (updated_at 없음).
 *   BaseTimeEntity 미상속, @CreatedDate 직접 사용.
 */
@Entity
@Table(
    name = "Favorite_Stops",
    uniqueConstraints = {
        // 동일 사용자가 동일 정류장을 중복 즐겨찾기하지 못하도록 제약
        @UniqueConstraint(columnNames = {"User_id", "Stop_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteStops {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Favorite_id")
    private Integer favoriteId;

    /**
     * 즐겨찾기 등록 사용자.
     * ✅ FetchType.LAZY: 즐겨찾기 조회 시 Users 데이터를 즉시 로딩하지 않음.
     * insertable/updatable = false: FK 값은 User_id 컬럼으로만 관리.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_user"))
    private Users user;

    /**
     * 즐겨찾기 정류장.
     * ✅ FetchType.LAZY: 즐겨찾기 조회 시 Stops 데이터를 즉시 로딩하지 않음.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Stop_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favorite_stop"))
    private Stops stop;

    /**
     * 사용자가 정류장에 붙인 별칭 (예: "우리집 앞 정류장").
     * NULL 허용.
     */
    @Column(name = "Alias", length = 50)
    private String alias;

    /**
     * 즐겨찾기 등록 시각.
     * updatable = false: 등록 후 변경 불가.
     */
    @CreatedDate
    @Column(name = "Created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
