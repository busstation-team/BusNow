package com.busnow.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * [테이블: Notification_Settings]
 * 도착 알림 설정 엔티티.
 *
 * ✅ FetchType.LAZY 강제: N+1 쿼리 방지.
 *    백그라운드 알림 체크 스케줄러에서 이 엔티티를 조회할 때
 *    Users/Stops/BusRoutes 데이터가 필요하면 JOIN FETCH 사용.
 *
 * - 감사 필드 없음 (명세서 DDL 기준).
 */
@Entity
@Table(name = "Notification_Settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Notification_id")
    private Integer notificationId;

    /**
     * 알림 설정 소유 사용자.
     * ✅ FetchType.LAZY
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_user"))
    private Users user;

    /**
     * 알림 대상 정류장.
     * ✅ FetchType.LAZY
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Stop_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_stop"))
    private Stops stop;

    /**
     * 알림 대상 노선.
     * ✅ FetchType.LAZY
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_route"))
    private BusRoutes busRoute;

    /**
     * 알림 발송 기준 시간 (초 단위).
     * 예: 180 → 버스 도착 3분 전에 알림.
     * 외부 API의 traTime(초)과 비교하는 핵심 비즈니스 로직 필드.
     */
    @Column(name = "Alert_time")
    private Integer alertTime;

    /**
     * 알림 활성화 여부. 기본값: TRUE.
     * FALSE 설정 시 스케줄러에서 해당 알림 설정을 건너뜀.
     */
    @Column(name = "Is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
