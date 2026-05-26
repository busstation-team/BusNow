package com.busnow.repository;

import com.busnow.entity.FavoriteStops;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Favorite_Stops 테이블 JPA Repository.
 *
 * ✅ JOIN FETCH 전략:
 *    FavoriteStops의 user, stop 필드가 FetchType.LAZY이므로
 *    목록 조회 시 N+1 방지를 위해 JPQL JOIN FETCH 사용.
 *    → 즐겨찾기 목록 + 정류소 정보를 단 1개 쿼리로 조회.
 */
public interface FavoriteStopsRepository extends JpaRepository<FavoriteStops, Integer> {

    /**
     * 특정 사용자의 즐겨찾기 목록 전체 조회.
     * ✅ JOIN FETCH: stop 정보를 함께 로딩하여 N+1 방지.
     *
     * @param userId Users.User_id
     * @return 해당 사용자의 즐겨찾기 목록 (Stops 정보 포함)
     */
    @Query("""
            SELECT f FROM FavoriteStops f
            JOIN FETCH f.stop
            WHERE f.user.userId = :userId
            ORDER BY f.createdAt DESC
            """)
    List<FavoriteStops> findByUserIdWithStop(@Param("userId") Integer userId);

    /**
     * 즐겨찾기 단건 조회 (소유권 검증 포함).
     * 본인 즐겨찾기인지 확인할 때 사용.
     *
     * @param favoriteId 즐겨찾기 ID
     * @param userId     요청 사용자 ID
     */
    Optional<FavoriteStops> findByFavoriteIdAndUserUserId(Integer favoriteId, Integer userId);

    /**
     * 중복 등록 여부 확인.
     * 동일 사용자가 동일 정류소를 다시 즐겨찾기하려는 경우 방지.
     *
     * @param userId Users.User_id
     * @param stopId Stops.Stop_id
     */
    boolean existsByUserUserIdAndStopStopId(Integer userId, String stopId);

    /**
     * 특정 사용자의 특정 정류소 즐겨찾기 삭제.
     * stopId만으로 삭제할 때 사용 (favoriteId 미보유 시).
     */
    void deleteByUserUserIdAndStopStopId(Integer userId, String stopId);

    /**
     * 즐겨찾기 등록된 모든 Stop_id 목록 조회 (메인 대시보드 폴링용).
     *
     * @param userId Users.User_id
     * @return Stop_id 문자열 목록
     */
    @Query("""
            SELECT f.stop.stopId FROM FavoriteStops f
            WHERE f.user.userId = :userId
            """)
    List<String> findStopIdsByUserId(@Param("userId") Integer userId);
}
