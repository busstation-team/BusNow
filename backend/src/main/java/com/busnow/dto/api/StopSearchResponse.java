package com.busnow.dto.api;

/**
 * 정류소 검색 결과 응답 DTO (외부 API 연동용).
 *
 * @param stopId   국토교통부 nodeId
 * @param stopName 정류소명
 * @param stopNo   정류소 번호 (5자리)
 * @param cityCode 도시 코드
 */
public record StopSearchResponse(
        String stopId,
        String stopName,
        String stopNo,
        Integer cityCode
) {}
