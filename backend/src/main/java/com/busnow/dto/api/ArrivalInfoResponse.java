package com.busnow.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 프론트엔드로 전달하는 버스 도착 정보 응답 DTO.
 *
 *  설계 원칙:
 *    - BusArrivalItem (외부 API raw 응답)을 직접 노출하지 않고,
 *      프론트엔드에 필요한 데이터만 선별하여 반환 (API 응답 캡슐화).
 *    - 필드명은 백엔드 DB Pascal/Snake 컨벤션을 따르지 않고
 *      프론트엔드 가독성 기준으로 camelCase 사용 (응답 DTO 한정).
 *
 * @param routeId         노선 ID (DB Bus_Routes.Route_id 매핑용)
 * @param routeNo         노선 번호 (예: "330", "직행좌석")
 * @param routeType       노선 유형 (예: "일반버스")
 * @param predictTime     도착 예정 시간 (분). null이면 정보 없음.
 * @param traTime         도착 예정 시간 (초). Notification_Settings.Alert_time 비교용.
 * @param remainSeatCnt   남은 좌석 수. -1이면 정보 없음.
 * @param remainStopCnt   남은 정류소 수 (staOrd)
 * @param vehicleNo       차량 번호
 * @param arrivalMessage  도착 안내 메시지 (arrmsg1 기반, 가공된 형태)
 * @param nextArrivalMsg  다음 버스 도착 메시지 (arrmsg2)
 * @param stopId          정류소 ID
 * @param stopName        정류소 명칭
 * @param isImminent      긴급 도착 여부 (predictTime <= 3분 또는 "곧 도착" 메시지)
 */
public record ArrivalInfoResponse(
        @JsonProperty("routeId") String routeId,
        @JsonProperty("routeNo") String routeNo,
        @JsonProperty("routeType") String routeType,
        @JsonProperty("predictTime") Integer predictTime,
        @JsonProperty("traTime") Integer traTime,
        @JsonProperty("remainStopCnt") Integer remainStopCnt,
        @JsonProperty("arrivalMessage") String arrivalMessage,
        @JsonProperty("stopId") String stopId,
        @JsonProperty("stopName") String stopName,
        @JsonProperty("isImminent") boolean isImminent
) {
    /**
     * BusArrivalItem(외부 API raw 응답)을 프론트엔드용 DTO로 변환하는 팩토리 메서드.
     *
     * @param item 국토교통부 API 응답 개별 항목
     * @return 프론트엔드용 도착 정보 DTO
     */
    public static ArrivalInfoResponse from(BusArrivalItem item) {
        // 초 단위를 분 단위로 변환 (프론트엔드 표시용)
        Integer minutes = (item.arrTime() != null) ? item.arrTime() / 60 : null;
        
        // 긴급 도착 판단: 3분(180초) 이하
        boolean imminent = (item.arrTime() != null && item.arrTime() <= 180);

        return new ArrivalInfoResponse(
                item.routeId(),
                item.routeNo(),
                item.routetp(),
                minutes,
                item.arrTime(),
                item.arrPrevStationCnt(),
                formatTagoMessage(minutes, item.arrPrevStationCnt()),
                item.nodeId(),
                item.nodeName(),
                imminent
        );
    }

    /**
     * TAGO 데이터를 바탕으로 도착 메시지 생성.
     * 예: "약 3분 후 [2번째 전]"
     */
    private static String formatTagoMessage(Integer minutes, Integer prevCount) {
        if (minutes == null) return "정보 없음";
        if (minutes == 0) return "잠시 후 도착";
        
        StringBuilder sb = new StringBuilder();
        sb.append("약 ").append(minutes).append("분 후");
        if (prevCount != null && prevCount > 0) {
            sb.append(" [").append(prevCount).append("번째 전]");
        }
        return sb.toString();
    }
}
