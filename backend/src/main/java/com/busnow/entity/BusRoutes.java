package com.busnow.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * [테이블: Bus_Routes]
 * 버스 노선 마스터 데이터 엔티티.
 *
 * - PK가 VARCHAR 타입이므로 @GeneratedValue 없이 직접 할당.
 * - CSV 적재 시 JdbcTemplate.batchUpdate 사용.
 */
@Entity
@Table(name = "Bus_Routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusRoutes {

    /**
     * 국토교통부 API의 노선 고유 ID.
     * VARCHAR(30), 수동 할당.
     */
    @Id
    @Column(name = "Route_id", length = 30)
    private String routeId;

    /**
     * 노선 번호 또는 이름 (예: "330", "직행좌석").
     */
    @Column(name = "Route_name", nullable = false, length = 50)
    private String routeName;

    /**
     * 노선 유형 (예: "일반버스", "좌석버스", "마을버스").
     */
    @Column(name = "Route_type", length = 20)
    private String routeType;
}
