package com.busnow.dto.notification;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 알림 설정 등록/수정 요청 DTO.
 *
 * @param stopId    알림 대상 정류소 ID (Notification_Settings.Stop_id)
 * @param stopName  정류소 명칭 (DB에 없을 경우 자동 생성을 위함)
 * @param routeId   알림 대상 노선 ID  (Notification_Settings.Route_id)
 * @param routeName 노선 번호/이름 (DB에 없을 경우 자동 생성을 위함)
 * @param routeType 노선 유형 (DB에 없을 경우 자동 생성을 위함)
 * @param alertTime 알림 기준 시간 (초). 예: 180 = 3분 전 알림. 최소 30초.
 * @param isActive  알림 활성화 여부 (등록 시 기본값 true)
 */
public record NotificationSettingRequest(
        @NotBlank(message = "정류소 ID를 입력해주세요.")
        String stopId,

        @NotBlank(message = "정류소 명칭을 입력해주세요.")
        String stopName,

        @NotBlank(message = "노선 ID를 입력해주세요.")
        String routeId,

        @NotBlank(message = "노선 번호를 입력해주세요.")
        String routeName,

        String routeType,

        @NotNull(message = "알림 시간을 설정해주세요.")
        @Min(value = 30, message = "알림 시간은 최소 30초 이상이어야 합니다.")
        Integer alertTime,

        Boolean isActive
) {}
