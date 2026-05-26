package com.busnow.controller;

import com.busnow.dto.api.StopArrivalResponse;
import com.busnow.dto.favorite.FavoriteStopRequest;
import com.busnow.dto.favorite.FavoriteStopResponse;
import com.busnow.security.JwtTokenProvider;
import com.busnow.service.favorite.FavoriteStopsService;
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
 * 즐겨찾기 정류소 REST 컨트롤러.
 * Base URL: /api/favorites
 *
 *  모든 엔드포인트는 JWT 인증 필수 (SecurityConfig에서 /favorites/** 보호).
 *  userId는 Authorization 헤더의 JWT에서 추출 (요청 바디/파라미터 미사용).
 *    → 사용자 ID 위조(파라미터 조작) 공격 방지.
 */
@Slf4j
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteStopsController {

    private final FavoriteStopsService favoriteStopsService;
    private final JwtTokenProvider jwtTokenProvider;

    // ============================================================
    // GET /api/favorites - 즐겨찾기 목록 조회
    // ============================================================
    @GetMapping
    public ResponseEntity<List<FavoriteStopResponse>> getFavorites(HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return ResponseEntity.ok(favoriteStopsService.getFavorites(userId));
    }

    // ============================================================
    // GET /api/favorites/arrival?cityCode=37070 - 즐겨찾기 + 실시간 도착 정보
    // ============================================================

    /**
     * 프론트엔드 메인 대시보드 10초 폴링 API.
     * 즐겨찾기 정류소의 실시간 도착 정보를 일괄 반환.
     *
     * @param cityCode 도시 코드 (기본값: 37070 = 성남시)
     */
    @GetMapping("/arrival")
    public ResponseEntity<List<StopArrivalResponse>> getFavoritesWithArrival(
            HttpServletRequest request,
            @RequestParam(value = "cityCode", defaultValue = "31190") String cityCode
    ) {
        Integer userId = extractUserId(request);
        List<StopArrivalResponse> result =
                favoriteStopsService.getFavoritesWithArrivalInfo(userId, cityCode);
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // POST /api/favorites - 즐겨찾기 등록
    // ============================================================
    @PostMapping
    public ResponseEntity<FavoriteStopResponse> addFavorite(
            HttpServletRequest request,
            @Valid @RequestBody FavoriteStopRequest body
    ) {
        Integer userId = extractUserId(request);
        FavoriteStopResponse response = favoriteStopsService.addFavorite(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // PATCH /api/favorites/{favoriteId}/alias - 별칭 수정
    // ============================================================
    @PatchMapping("/{favoriteId}/alias")
    public ResponseEntity<FavoriteStopResponse> updateAlias(
            HttpServletRequest request,
            @PathVariable Integer favoriteId,
            @RequestBody Map<String, String> body  // { "alias": "새 별칭" }
    ) {
        Integer userId = extractUserId(request);
        String newAlias = body.get("alias");
        FavoriteStopResponse response =
                favoriteStopsService.updateAlias(userId, favoriteId, newAlias);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // DELETE /api/favorites/{favoriteId} - 즐겨찾기 삭제 (ID 기준)
    // ============================================================
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Map<String, String>> removeFavorite(
            HttpServletRequest request,
            @PathVariable Integer favoriteId
    ) {
        Integer userId = extractUserId(request);
        favoriteStopsService.removeFavorite(userId, favoriteId);
        return ResponseEntity.ok(Map.of("message", "즐겨찾기가 삭제되었습니다."));
    }

    // ============================================================
    // DELETE /api/favorites/stop/{stopId} - 즐겨찾기 삭제 (Stop_id 기준)
    // ============================================================
    @DeleteMapping("/stop/{stopId}")
    public ResponseEntity<Map<String, String>> removeFavoriteByStop(
            HttpServletRequest request,
            @PathVariable String stopId
    ) {
        Integer userId = extractUserId(request);
        favoriteStopsService.removeFavoriteByStopId(userId, stopId);
        return ResponseEntity.ok(Map.of("message", "즐겨찾기가 삭제되었습니다."));
    }

    // ============================================================
    // 내부 유틸리티: JWT에서 userId 추출
    // ============================================================

    /**
     * Authorization 헤더의 Access Token에서 userId 클레임 추출.
     * JwtAuthenticationFilter가 이미 검증 완료한 토큰이므로 재검증 불필요.
     */
    private Integer extractUserId(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        String token = bearer.substring(7); // "Bearer " 제거
        return jwtTokenProvider.getUserId(token);
    }
}
