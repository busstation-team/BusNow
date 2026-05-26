package com.busnow.dto.api;

import java.util.Collections;
import java.util.List;

/**
 * 정류소 검색 + 도착 정보를 통합한 응답 DTO.
 *
 * 프론트엔드의 검색 결과 화면에서 사용.
 * 정류소 기본 정보 + 해당 정류소의 도착 버스 목록을 함께 전달.
 *
 * @param stopId       정류소 ID
 * @param stopName     정류소 명칭
 * @param arrivals     해당 정류소의 버스 도착 정보 목록 (없으면 빈 리스트)
 * @param totalCount   도착 예정 버스 수
 * @param hasError     외부 API 오류 여부 (true면 arrivals는 빈 리스트)
 * @param errorMessage 오류 메시지 (hasError=true일 때)
 */
public record StopArrivalResponse(
        String stopId,
        String stopName,
        List<ArrivalInfoResponse> arrivals,
        int totalCount,
        boolean hasError,
        String errorMessage
) {
    public static StopArrivalResponse success(String stopId, String stopName,
                                               List<ArrivalInfoResponse> arrivals) {
        return new StopArrivalResponse(stopId, stopName, arrivals, arrivals.size(), false, null);
    }

    public static StopArrivalResponse error(String stopId, String stopName, String errorMessage) {
        return new StopArrivalResponse(stopId, stopName, Collections.emptyList(), 0, true, errorMessage);
    }

    public static StopArrivalResponse empty(String stopId, String stopName) {
        return new StopArrivalResponse(stopId, stopName, Collections.emptyList(), 0, false, null);
    }
}
