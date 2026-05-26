package com.busnow.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * [테이블: Stops]
 * 버스 정류장 마스터 데이터 엔티티.
 *
 * - PK가 VARCHAR 타입이므로 @GeneratedValue 없이 직접 할당.
 * - CSV 적재 시 JdbcTemplate.batchUpdate를 사용하므로 saveAll 호출 없음.
 * - BaseTimeEntity를 상속하지 않음 (마스터 데이터 - 감사 컬럼 불필요).
 */
@Entity
@Table(name = "Stops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stops {

    /**
     * 국토교통부 API의 정류장 고유 ID (예: "BSS090040006").
     * VARCHAR(20), 수동 할당.
     */
    @Id
    @Column(name = "Stop_id", length = 20)
    private String stopId;

    /**
     * 정류장 명칭 (예: "가천대학교앞").
     */
    @Column(name = "Stop_name", nullable = false, length = 100)
    private String stopName;
}
