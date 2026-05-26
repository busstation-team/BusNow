package com.busnow.service.notification;

import com.busnow.dto.notification.NotificationSettingRequest;
import com.busnow.dto.notification.NotificationSettingResponse;
import com.busnow.entity.BusRoutes;
import com.busnow.entity.NotificationSettings;
import com.busnow.entity.Stops;
import com.busnow.entity.Users;
import com.busnow.repository.BusRoutesRepository;
import com.busnow.repository.NotificationSettingsRepository;
import com.busnow.repository.StopsRepository;
import com.busnow.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 도착 알림 설정 도메인 서비스.
 *
 *  주요 시나리오:
 *    1. 알림 등록: 정류소/노선 존재 확인 → 중복 검증 → NotificationSettings 저장
 *    2. 알림 목록 조회: JOIN FETCH로 stop/busRoute 함께 로딩
 *    3. 알림 수정: alertTime 변경
 *    4. 활성화/비활성화 토글: Is_active 플래그 반전
 *    5. 알림 삭제: 소유권 검증 후 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;
    private final StopsRepository stopsRepository;
    private final BusRoutesRepository busRoutesRepository;
    private final UsersRepository usersRepository;
    private final com.busnow.repository.FavoriteStopsRepository favoriteStopsRepository;

    // ============================================================
    // 알림 설정 등록
    // ============================================================

    /**
     * 알림 설정 신규 등록.
     *
     * @param userId  인증된 사용자 ID
     * @param request 알림 설정 요청 DTO
     * @return 등록된 알림 설정 응답 DTO
     */
    @Transactional
    public NotificationSettingResponse addNotification(Integer userId,
                                                        NotificationSettingRequest request) {
        // 1. 정류소 존재 확인 및 자동 생성
        Stops stop = stopsRepository.findById(request.stopId())
                .orElseGet(() -> {
                    log.info("[Notification] 새 정류소 마스터 등록: stopId={}, stopName={}", request.stopId(), request.stopName());
                    return stopsRepository.save(Stops.builder()
                            .stopId(request.stopId())
                            .stopName(request.stopName())
                            .build());
                });

        // 2. 노선 존재 확인 및 자동 생성
        BusRoutes route = busRoutesRepository.findById(request.routeId())
                .orElseGet(() -> {
                    log.info("[Notification] 새 노선 마스터 등록: routeId={}, routeName={}", request.routeId(), request.routeName());
                    return busRoutesRepository.save(BusRoutes.builder()
                            .routeId(request.routeId())
                            .routeName(request.routeName())
                            .routeType(request.routeType())
                            .build());
                });

        // 3. 동일 정류소+노선 중복 알림 방지
        if (notificationSettingsRepository.existsByUserUserIdAndStopStopIdAndBusRouteRouteId(
                userId, request.stopId(), request.routeId())) {
            throw new IllegalArgumentException("이미 해당 노선에 알림이 설정되어 있습니다. (설정된 노선의 모든 버스에 대해 도착 알림이 제공됩니다)");
        }

        // 4. 사용자 프록시 참조
        Users userRef = usersRepository.getReferenceById(userId);

        // 5. 알림 설정 저장
        NotificationSettings notification = NotificationSettings.builder()
                .user(userRef)
                .stop(stop)
                .busRoute(route)
                .alertTime(request.alertTime())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        NotificationSettings saved = notificationSettingsRepository.save(notification);
        log.info("[Notification] 등록: userId={}, stopId={}, routeId={}, alertTime={}s",
                userId, request.stopId(), request.routeId(), request.alertTime());

        return NotificationSettingResponse.from(saved);
    }

    // ============================================================
    // 알림 목록 조회
    // ============================================================

    /**
     * 사용자의 전체 알림 설정 목록 조회.
     *  JOIN FETCH: stop, busRoute 함께 로딩.
     *
     * @param userId 인증된 사용자 ID
     */
    @Transactional(readOnly = true)
    public List<NotificationSettingResponse> getNotifications(Integer userId) {
        // 1. 해당 사용자의 즐겨찾기 별칭 목록 조회
        java.util.Map<String, String> aliasMap = favoriteStopsRepository.findByUserIdWithStop(userId)
                .stream()
                .filter(f -> f.getAlias() != null)
                .collect(java.util.stream.Collectors.toMap(
                        f -> f.getStop().getStopId(),
                        com.busnow.entity.FavoriteStops::getAlias
                ));

        // 2. 알림 설정 조회 및 별칭 매핑
        return notificationSettingsRepository.findByUserIdWithDetails(userId)
                .stream()
                .map(n -> NotificationSettingResponse.from(n, aliasMap.get(n.getStop().getStopId())))
                .toList();
    }

    // ============================================================
    // 알림 시간 수정
    // ============================================================

    /**
     * 알림 기준 시간(Alert_time) 수정.
     *
     * @param userId         소유권 검증용 사용자 ID
     * @param notificationId 수정할 알림 설정 ID
     * @param newAlertTime   새 알림 기준 시간 (초)
     */
    @Transactional
    public NotificationSettingResponse updateAlertTime(Integer userId,
                                                        Integer notificationId,
                                                        Integer newAlertTime) {
        if (newAlertTime < 30) {
            throw new IllegalArgumentException("알림 시간은 최소 30초 이상이어야 합니다.");
        }

        NotificationSettings notification = findOwnedNotification(userId, notificationId);
        notification.setAlertTime(newAlertTime);
        //  더티 체킹으로 자동 UPDATE
        log.info("[Notification] 시간 수정: id={}, newAlertTime={}s", notificationId, newAlertTime);

        return NotificationSettingResponse.from(notification);
    }

    // ============================================================
    // 활성화/비활성화 토글
    // ============================================================

    /**
     * 알림 활성화 상태 토글 (Is_active 반전).
     * 프론트엔드 알림 설정 페이지의 스위치 UI에서 사용.
     *
     * @param userId         소유권 검증용 사용자 ID
     * @param notificationId 토글할 알림 설정 ID
     * @return 변경 후 Is_active 값
     */
    @Transactional
    public boolean toggleActive(Integer userId, Integer notificationId) {
        NotificationSettings notification = findOwnedNotification(userId, notificationId);
        boolean newState = !notification.getIsActive();
        notification.setIsActive(newState);

        log.info("[Notification] 토글: id={}, isActive={}", notificationId, newState);
        return newState;
    }

    // ============================================================
    // 알림 삭제
    // ============================================================

    /**
     * 알림 설정 삭제.
     *
     * @param userId         소유권 검증용 사용자 ID
     * @param notificationId 삭제할 알림 설정 ID
     */
    @Transactional
    public void removeNotification(Integer userId, Integer notificationId) {
        NotificationSettings notification = findOwnedNotification(userId, notificationId);
        notificationSettingsRepository.delete(notification);
        log.info("[Notification] 삭제: userId={}, notificationId={}", userId, notificationId);
    }

    @Transactional
    public void removeNotificationByRoute(Integer userId, String stopId, String routeId) {
        notificationSettingsRepository.deleteByUserUserIdAndStopStopIdAndBusRouteRouteId(userId, stopId, routeId);
        log.info("[Notification] 알림 설정 삭제(Route) 완료: userId={}, stopId={}, routeId={}", userId, stopId, routeId);
    }

    // ============================================================
    // 내부 유틸리티
    // ============================================================

    /**
     * 소유권 검증: 해당 알림이 요청 사용자의 것인지 확인.
     * 없거나 타인의 알림이면 IllegalArgumentException 발생.
     */
    private NotificationSettings findOwnedNotification(Integer userId, Integer notificationId) {
        return notificationSettingsRepository
                .findByNotificationIdAndUserUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "알림 설정을 찾을 수 없습니다. (ID: " + notificationId + ")"
                ));
    }
}
