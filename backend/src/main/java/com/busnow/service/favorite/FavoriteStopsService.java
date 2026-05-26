package com.busnow.service.favorite;

import com.busnow.dto.favorite.FavoriteStopRequest;
import com.busnow.dto.favorite.FavoriteStopResponse;
import com.busnow.dto.api.StopArrivalResponse;
import com.busnow.entity.FavoriteStops;
import com.busnow.entity.Stops;
import com.busnow.entity.Users;
import com.busnow.repository.FavoriteStopsRepository;
import com.busnow.repository.StopsRepository;
import com.busnow.repository.UsersRepository;
import com.busnow.service.bus.BusArrivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 즐겨찾기 정류소 도메인 서비스.
 *
 *  주요 시나리오:
 *    1. 즐겨찾기 등록: 정류소 존재 확인 → 중복 검증 → FavoriteStops 저장
 *    2. 즐겨찾기 목록: JOIN FETCH로 N+1 없이 조회 → DTO 변환
 *    3. 별칭 수정: 소유권 검증 → alias 업데이트
 *    4. 즐겨찾기 삭제: 소유권 검증 → 삭제
 *    5. 메인 대시보드: 즐겨찾기 Stop_id 목록 → BusArrivalService 일괄 호출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteStopsService {

    private final FavoriteStopsRepository favoriteStopsRepository;
    private final StopsRepository stopsRepository;
    private final UsersRepository usersRepository;
    private final BusArrivalService busArrivalService;

    // ============================================================
    // 즐겨찾기 등록
    // ============================================================

    /**
     * 즐겨찾기 정류소 등록.
     *
     * @param userId  인증된 사용자 ID (JWT 클레임에서 추출)
     * @param request 등록 요청 DTO (stopId, alias)
     * @return 등록된 즐겨찾기 응답 DTO
     */
    @Transactional
    public FavoriteStopResponse addFavorite(Integer userId, FavoriteStopRequest request) {
        // 1. 정류소 존재 확인 및 자동 생성 (Upsert 패턴)
        Stops stop = stopsRepository.findById(request.stopId())
                .orElseGet(() -> {
                    log.info("[Favorite] 새 정류소 마스터 등록: stopId={}, stopName={}", request.stopId(), request.stopName());
                    Stops newStop = Stops.builder()
                            .stopId(request.stopId())
                            .stopName(request.stopName())
                            .build();
                    return stopsRepository.save(newStop);
                });

        // 2. 중복 등록 방지
        if (favoriteStopsRepository.existsByUserUserIdAndStopStopId(userId, request.stopId())) {
            throw new IllegalArgumentException("이미 즐겨찾기에 등록된 정류소입니다.");
        }

        // 3. 사용자 엔티티 참조 (LAZY 연관관계 설정용)
        Users userRef = usersRepository.getReferenceById(userId); // SELECT 없이 프록시 생성

        // 4. 즐겨찾기 저장
        FavoriteStops favorite = FavoriteStops.builder()
                .user(userRef)
                .stop(stop)
                .alias(request.alias())
                .build();

        FavoriteStops saved = favoriteStopsRepository.save(favorite);
        log.info("[Favorite] 등록: userId={}, stopId={}, alias={}", userId, request.stopId(), request.alias());

        return FavoriteStopResponse.from(saved);
    }

    // ============================================================
    // 즐겨찾기 목록 조회
    // ============================================================

    /**
     * 사용자의 즐겨찾기 목록 조회.
     *  JOIN FETCH로 N+1 방지 (Repository 쿼리에서 처리).
     *
     * @param userId 인증된 사용자 ID
     * @return 즐겨찾기 목록 DTO
     */
    @Transactional(readOnly = true)
    public List<FavoriteStopResponse> getFavorites(Integer userId) {
        return favoriteStopsRepository.findByUserIdWithStop(userId)
                .stream()
                .map(FavoriteStopResponse::from)  //  트랜잭션 내에서 LAZY 필드 접근
                .toList();
    }

    // ============================================================
    // 별칭 수정
    // ============================================================

    /**
     * 즐겨찾기 별칭 수정.
     *
     * @param userId     인증된 사용자 ID (소유권 검증용)
     * @param favoriteId 즐겨찾기 ID
     * @param newAlias   새 별칭 (null이면 별칭 제거)
     * @return 수정된 즐겨찾기 응답 DTO
     */
    @Transactional
    public FavoriteStopResponse updateAlias(Integer userId, Integer favoriteId, String newAlias) {
        FavoriteStops favorite = favoriteStopsRepository
                .findByFavoriteIdAndUserUserId(favoriteId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "즐겨찾기를 찾을 수 없습니다. (ID: " + favoriteId + ")"
                ));

        favorite.setAlias(newAlias);
        //  더티 체킹: @Transactional 내에서 엔티티 변경 시 flush 시점에 자동 UPDATE
        log.info("[Favorite] 별칭 수정: favoriteId={}, newAlias={}", favoriteId, newAlias);

        return FavoriteStopResponse.from(favorite);
    }

    // ============================================================
    // 즐겨찾기 삭제
    // ============================================================

    /**
     * 즐겨찾기 삭제 (favoriteId 기준).
     *
     * @param userId     소유권 검증용 사용자 ID
     * @param favoriteId 삭제할 즐겨찾기 ID
     */
    @Transactional
    public void removeFavorite(Integer userId, Integer favoriteId) {
        FavoriteStops favorite = favoriteStopsRepository
                .findByFavoriteIdAndUserUserId(favoriteId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "즐겨찾기를 찾을 수 없습니다. (ID: " + favoriteId + ")"
                ));

        favoriteStopsRepository.delete(favorite);
        log.info("[Favorite] 삭제: userId={}, favoriteId={}", userId, favoriteId);
    }

    /**
     * 즐겨찾기 삭제 (stopId 기준, 편의 메서드).
     *
     * @param userId 소유권 검증용 사용자 ID
     * @param stopId 삭제할 정류소 ID
     */
    @Transactional
    public void removeFavoriteByStopId(Integer userId, String stopId) {
        if (!favoriteStopsRepository.existsByUserUserIdAndStopStopId(userId, stopId)) {
            throw new IllegalArgumentException("즐겨찾기에 등록되지 않은 정류소입니다.");
        }
        favoriteStopsRepository.deleteByUserUserIdAndStopStopId(userId, stopId);
        log.info("[Favorite] 삭제(stopId): userId={}, stopId={}", userId, stopId);
    }

    // ============================================================
    // 메인 대시보드: 즐겨찾기 도착 정보 일괄 조회
    // ============================================================

    /**
     * 사용자의 즐겨찾기 정류소 전체의 실시간 도착 정보 조회.
     * 프론트엔드 메인 대시보드에서 10초마다 폴링하는 핵심 API.
     *
     *  흐름:
     *    1. 즐겨찾기 Stop_id 목록 조회 (단순 ID만, JOIN 없이)
     *    2. BusArrivalService.getArrivalInfoBatch() 호출 (외부 API 연동)
     *    3. 결과 반환 (외부 API 실패한 정류소는 hasError=true로 표시)
     *
     * @param userId   인증된 사용자 ID
     * @param cityCode 도시 코드 (기본값: "37070" = 성남시)
     * @return 즐겨찾기 정류소별 도착 정보 목록
     */
    @Transactional(readOnly = true)
    public List<StopArrivalResponse> getFavoritesWithArrivalInfo(Integer userId, String cityCode) {
        // 1. 즐겨찾기 정류소 ID 목록 조회
        List<String> stopIds = favoriteStopsRepository.findStopIdsByUserId(userId);
        log.info("[Favorite] 사용자 {}의 즐겨찾기 ID 수: {}개", userId, stopIds.size());

        if (stopIds.isEmpty()) return List.of();

        // 2. 외부 API 일괄 호출
        List<StopArrivalResponse> arrivals = busArrivalService.getArrivalInfoBatch(stopIds, cityCode);

        // 3. 별칭(Alias) 정보 가져오기
        List<FavoriteStops> favorites = favoriteStopsRepository.findByUserIdWithStop(userId);
        Map<String, String> aliasMap = favorites.stream()
                .filter(f -> f.getAlias() != null)
                .collect(Collectors.toMap(f -> f.getStop().getStopId(), FavoriteStops::getAlias, (a, b) -> a));

        // 4. 별칭 적용
        if (aliasMap.isEmpty()) return arrivals;

        return arrivals.stream()
                .map(a -> {
                    String alias = aliasMap.get(a.stopId());
                    if (alias == null) return a;
                    return new StopArrivalResponse(a.stopId(), alias, a.arrivals(), a.totalCount(), a.hasError(), a.errorMessage());
                })
                .toList();
    }
}
