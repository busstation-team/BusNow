package com.busnow.controller;

import com.busnow.dto.notification.NotificationSettingRequest;
import com.busnow.dto.notification.NotificationSettingResponse;
import com.busnow.security.JwtTokenProvider;
import com.busnow.service.notification.NotificationSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 도착 알림 설정 REST 컨트롤러.
 * Base URL: /api/notifications
 *
 *  모든 엔드포인트 JWT 인증 필수.
 *  userId는 JWT 클레임에서 추출 (파라미터 위조 방지).
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;
    private final JwtTokenProvider jwtTokenProvider;

    // ============================================================
    // GET /api/notifications - 알림 설정 목록 조회
    // ============================================================
    @GetMapping
    public ResponseEntity<List<NotificationSettingResponse>> getNotifications(
            HttpServletRequest request
    ) {
        Integer userId = extractUserId(request);
        return ResponseEntity.ok(notificationSettingsService.getNotifications(userId));
    }

    // ============================================================
    // POST /api/notifications - 알림 설정 등록
    // ============================================================
    @PostMapping
    public ResponseEntity<NotificationSettingResponse> addNotification(
            HttpServletRequest request,
            @Valid @RequestBody NotificationSettingRequest body
    ) {
        Integer userId = extractUserId(request);
        NotificationSettingResponse response =
                notificationSettingsService.addNotification(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // PATCH /api/notifications/{notificationId}/alert-time - 알림 시간 수정
    // ============================================================
    @PatchMapping("/{notificationId}/alert-time")
    public ResponseEntity<NotificationSettingResponse> updateAlertTime(
            HttpServletRequest request,
            @PathVariable Integer notificationId,
            @RequestBody Map<String, Integer> body  // { "alertTime": 180 }
    ) {
        Integer userId = extractUserId(request);
        Integer newAlertTime = body.get("alertTime");

        if (newAlertTime == null) {
            return ResponseEntity.badRequest().build();
        }

        NotificationSettingResponse response =
                notificationSettingsService.updateAlertTime(userId, notificationId, newAlertTime);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // PATCH /api/notifications/{notificationId}/toggle - 활성화/비활성화 토글
    // ============================================================

    /**
     * 알림 활성화 상태 토글.
     * 프론트엔드 스위치 UI에서 사용.
     * 응답: { "isActive": true/false }
     */
    @PatchMapping("/{notificationId}/toggle")
    public ResponseEntity<Map<String, Boolean>> toggleActive(
            HttpServletRequest request,
            @PathVariable Integer notificationId
    ) {
        Integer userId = extractUserId(request);
        boolean newState = notificationSettingsService.toggleActive(userId, notificationId);
        return ResponseEntity.ok(Map.of("isActive", newState));
    }

    // ============================================================
    // DELETE /api/notifications/{notificationId} - 알림 설정 삭제
    // ============================================================
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> removeNotification(
            HttpServletRequest request,
            @PathVariable Integer notificationId
    ) {
        Integer userId = extractUserId(request);
        notificationSettingsService.removeNotification(userId, notificationId);
        return ResponseEntity.ok(Map.of("message", "알림 설정이 삭제되었습니다."));
    }

    // ============================================================
    // DELETE /api/notifications/route - 알림 설정 삭제 (stopId, routeId 기반)
    // ============================================================
    @DeleteMapping("/route")
    public ResponseEntity<Map<String, String>> removeNotificationByRoute(
            HttpServletRequest request,
            @RequestParam String stopId,
            @RequestParam String routeId
    ) {
        Integer userId = extractUserId(request);
        notificationSettingsService.removeNotificationByRoute(userId, stopId, routeId);
        return ResponseEntity.ok(Map.of("message", "알림 설정이 삭제되었습니다."));
    }

    // ============================================================
    // 내부 유틸리티
    // ============================================================
    private Integer extractUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        String token = bearer.substring(7);
        return jwtTokenProvider.getUserId(token);
    }
}
