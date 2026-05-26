package com.busnow.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 국토교통부 버스 도착 예정 정보 API - 개별 도착 정보 항목.
 *
 *  외부 API 응답 필드 (JSON) → Java record 매핑.
 *    @JsonProperty: API의 camelCase 필드명을 명시적으로 지정.
 *    명세서 규칙: 외부 API 응답은 @JsonProperty 매핑 사용.
 *
 *  @JsonIgnoreProperties(ignoreUnknown = true):
 *    API 버전 업데이트로 새 필드가 추가되어도 역직렬화 실패 방지.
 *
 * 실제 API 응답 필드 (국토교통부 정류소별도착예정정보목록조회):
 * - routeId    : 노선 ID (Bus_Routes.Route_id와 매핑)
 * - routeNo    : 노선 번호 (예: "330")
 * - predictTime: 도착 예정 시간 (분 단위)
 * - remainSeatCnt: 남은 좌석 수 (-1: 정보 없음)
 * - staOrd     : 남은 정류소 수
 * - vehicleNo  : 차량 번호
 * - arrmsg1    : 첫 번째 버스 도착 안내 메시지 (예: "3분후[2번째전]")
 * - arrmsg2    : 두 번째 버스 도착 안내 메시지
 * - nodeId     : 정류소 ID (Stops.Stop_id와 매핑)
 * - nodeName   : 정류소 명칭
 * - routetp    : 노선 유형
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusArrivalItem(
        @JsonProperty("routeid") String routeId,
        @JsonProperty("routeno") String routeNo,
        @JsonProperty("arrtime") Integer arrTime,
        @JsonProperty("arrprevstationcnt") Integer arrPrevStationCnt,
        @JsonProperty("nodeid") String nodeId,
        @JsonProperty("nodenm") String nodeName,
        @JsonProperty("routetp") String routetp
) {}
