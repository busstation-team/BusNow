package com.busnow.controller;

import com.busnow.dto.api.StopArrivalResponse;
import com.busnow.dto.api.StopSearchResponse;
import com.busnow.service.bus.BusArrivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 버스 도착 정보 및 정류소 검색 REST 컨트롤러.
 * Base URL: /api/bus, /api/stops
 *
 *  SecurityConfig에서 공개(인증 불필요) 엔드포인트:
 *    - GET /api/stops/search    (비로그인 허용)
 *    - GET /api/bus/arrival/**  (비로그인 허용)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BusArrivalController {

    private final BusArrivalService busArrivalService;

    // ============================================================
    // GET /api/stops/search?keyword=가천대 - 정류소 검색
    // ============================================================

    /**
     * 정류소 이름 키워드 검색.
     * 외부 API 연동.
     *
     * @param keyword 검색어 (예: "가천대", "야탑")
     * @return 매칭된 정류소 목록
     */
    @GetMapping("/stops/search")
    public ResponseEntity<List<StopSearchResponse>> searchStops(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer cityCode
    ) {
        log.info("[Search] 정류소 검색: {} (cityCode: {})", keyword, cityCode);
        return ResponseEntity.ok(busArrivalService.searchStopsViaApi(keyword, cityCode));
    }

    // ============================================================
    // GET /api/bus/arrival/{stopId} - 단일 정류소 도착 정보 조회
    // ============================================================

    /**
     * 특정 정류소의 실시간 버스 도착 정보 조회.
     * 국토교통부 API를 통해 실시간 데이터 조회.
     *
     * @param stopId   정류소 ID (Stops.Stop_id)
     * @param cityCode 도시 코드 (기본값: 37070 = 성남시 경기도)
     * @return 도착 정보 응답 (오류 시에도 200 반환, hasError 필드로 판별)
     */
    @GetMapping("/bus/arrival/{stopId}")
    public ResponseEntity<StopArrivalResponse> getArrivalInfo(
            @PathVariable String stopId,
            @RequestParam(value = "cityCode", defaultValue = "31190") String cityCode
    ) {
        log.info("[Controller] 도착 정보 조회: stopId={}", stopId);
        StopArrivalResponse response = busArrivalService.getArrivalInfo(stopId, cityCode);
        log.debug("[Controller] Final Response to Frontend: {}", response);
        return ResponseEntity.ok(response);
    }


    // ============================================================
    // POST /api/bus/arrival/batch - 즐겨찾기 정류소 일괄 조회
    // ============================================================

    /**
     * 즐겨찾기 정류소 ID 목록의 도착 정보 일괄 조회.
     * 프론트엔드 메인 대시보드의 10초 폴링 API.
     *
     *  POST 방식 선택 이유:
     *    - 조회 대상 stopId 목록이 가변적이므로 GET 쿼리 파라미터보다 바디 전송이 명확.
     *    - 목록이 많아질 경우 URL 길이 제한 회피.
     *
     * @param request { "stopIds": ["BSS090040001", ...], "cityCode": "37070" }
     * @return 각 정류소별 도착 정보 목록
     */
    @PostMapping("/bus/arrival/batch")
    public ResponseEntity<List<StopArrivalResponse>> getArrivalInfoBatch(
            @RequestBody BatchArrivalRequest request
    ) {
        log.info("[Controller] 즐겨찾기 일괄 조회: {}개", request.stopIds().size());
        List<StopArrivalResponse> responses = busArrivalService.getArrivalInfoBatch(
                request.stopIds(),
                request.cityCode() != null ? request.cityCode() : "31190"
        );
        return ResponseEntity.ok(responses);
    }

    /**
     * 일괄 조회 요청 DTO (컨트롤러 내부 private record).
     * 컨트롤러 전용이므로 별도 파일로 분리하지 않음.
     */
    private record BatchArrivalRequest(
            List<String> stopIds,
            String cityCode
    ) {}
}
