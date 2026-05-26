package com.busnow.dto.notification;

import com.busnow.entity.NotificationSettings;

/**
 * 알림 설정 응답 DTO.
 *
 *  LAZY 로딩 주의:
 *    stop, busRoute가 LAZY이므로 반드시 @Transactional 내에서 변환.
 *
 * @param notificationId 알림 설정 ID
 * @param stopId         정류소 ID
 * @param stopName       정류소 명칭
 * @param routeId        노선 ID
 * @param routeName      노선 번호/이름
 * @param routeType      노선 유형
 * @param alertTime      알림 기준 시간 (초)
 * @param alertTimeMin   알림 기준 시간 (분, 화면 표시용)
 * @param isActive       알림 활성화 여부
 */
public record NotificationSettingResponse(
        Integer notificationId,
        String stopId,
        String stopName,
        String routeId,
        String routeName,
        String routeType,
        Integer alertTime,
        Integer alertTimeMin,
        Boolean isActive
) {
    /**
     * NotificationSettings 엔티티 → 응답 DTO 변환.
     *  트랜잭션 내에서 호출 필수.
     */
    public static NotificationSettingResponse from(NotificationSettings n) {
        return new NotificationSettingResponse(
                n.getNotificationId(),
                n.getStop().getStopId(),
                n.getStop().getStopName(),
                n.getBusRoute().getRouteId(),
                n.getBusRoute().getRouteName(),
                n.getBusRoute().getRouteType(),
                n.getAlertTime(),
                n.getAlertTime() != null ? n.getAlertTime() / 60 : null,  // 초 → 분 변환
                n.getIsActive()
        );
    }

    public static NotificationSettingResponse from(NotificationSettings n, String alias) {
        String displayName = alias != null ? alias : n.getStop().getStopName();
        return new NotificationSettingResponse(
                n.getNotificationId(),
                n.getStop().getStopId(),
                displayName,
                n.getBusRoute().getRouteId(),
                n.getBusRoute().getRouteName(),
                n.getBusRoute().getRouteType(),
                n.getAlertTime(),
                n.getAlertTime() != null ? n.getAlertTime() / 60 : null,  // 초 → 분 변환
                n.getIsActive()
        );
    }
}
