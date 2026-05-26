package com.busnow.repository;

import com.busnow.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Notification_Settings 테이블 JPA Repository.
 *
 * ✅ 알림 스케줄러 핵심 쿼리:
 *    findAllActiveWithDetails(): Is_active=true인 모든 설정을
 *    user/stop/busRoute JOIN FETCH로 한 번에 로딩.
 *    스케줄러가 주기적으로 호출하므로 N+1 절대 금지.
 */
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Integer> {

    /**
     * 특정 사용자의 알림 설정 목록 조회.
     * ✅ JOIN FETCH: stop, busRoute 함께 로딩 (N+1 방지).
     *
     * @param userId Users.User_id
     */
    @Query("""
            SELECT n FROM NotificationSettings n
            JOIN FETCH n.stop
            JOIN FETCH n.busRoute
            WHERE n.user.userId = :userId
            ORDER BY n.notificationId DESC
            """)
    List<NotificationSettings> findByUserIdWithDetails(@Param("userId") Integer userId);

    /**
     * 알림 단건 조회 (소유권 검증 포함).
     */
    Optional<NotificationSettings> findByNotificationIdAndUserUserId(
            Integer notificationId, Integer userId
    );

    /**
     * 중복 알림 설정 확인.
     * 동일 사용자, 동일 정류소, 동일 노선 조합이 이미 존재하는지 확인.
     */
    boolean existsByUserUserIdAndStopStopIdAndBusRouteRouteId(
            Integer userId, String stopId, String routeId
    );

    /**
     * 특정 정류소 및 노선의 알림 삭제
     */
    void deleteByUserUserIdAndStopStopIdAndBusRouteRouteId(Integer userId, String stopId, String routeId);

    /**
     * 스케줄러 전용 - 모든 활성 알림 설정 조회.
     * ✅ JOIN FETCH user, stop, busRoute: 스케줄러에서 N+1 완전 차단.
     *    Is_active = true인 것만 로딩하여 불필요한 처리 제거.
     */
    @Query("""
            SELECT n FROM NotificationSettings n
            JOIN FETCH n.user
            JOIN FETCH n.stop
            JOIN FETCH n.busRoute
            WHERE n.isActive = true
            """)
    List<NotificationSettings> findAllActiveWithDetails();

    /**
     * 활성화/비활성화 토글 (벌크 UPDATE).
     */
    @Modifying
    @Query("""
            UPDATE NotificationSettings n
            SET n.isActive = :isActive
            WHERE n.notificationId = :id AND n.user.userId = :userId
            """)
    int updateIsActive(@Param("id") Integer notificationId,
                       @Param("userId") Integer userId,
                       @Param("isActive") Boolean isActive);
}
