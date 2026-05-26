package com.busnow.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 국토교통부 API 최상위 응답 래퍼.
 *
 *  국토교통부 공공 API 응답 JSON 구조:
 * {
 *   "response": {
 *     "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE" },
 *     "body": {
 *       "items": {
 *         "item": [ { ... }, { ... } ]   ← 단일 결과면 Object, 복수면 Array
 *       },
 *       "numOfRows": 10,
 *       "pageNo": 1,
 *       "totalCount": 5
 *     }
 *   }
 * }
 *
 *  record 계층 구조:
 *    BusArrivalApiResponse
 *     └── Response
 *          ├── Header
 *          └── Body
 *               └── Items
 *                    └── List<BusArrivalItem>
 *
 *  @JsonIgnoreProperties(ignoreUnknown = true):
 *    API 응답에 예상치 못한 필드가 있어도 역직렬화 실패 방지.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusArrivalApiResponse(

        @JsonProperty("response")
        Response response

) {

    // ----------------------------------------------------------
    // 중첩 record: Response
    // ----------------------------------------------------------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(

            @JsonProperty("header")
            Header header,

            @JsonProperty("body")
            Body body
    ) {}

    // ----------------------------------------------------------
    // 중첩 record: Header (API 응답 코드 확인용)
    // ----------------------------------------------------------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(

            @JsonProperty("resultCode")
            String resultCode,  // "00" = 정상, 그 외 = 오류

            @JsonProperty("resultMsg")
            String resultMsg    // "NORMAL SERVICE" 등
    ) {
        /** API 호출 성공 여부 */
        public boolean isSuccess() {
            return "00".equals(resultCode);
        }
    }

    // ----------------------------------------------------------
    // 중첩 record: Body
    // ----------------------------------------------------------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(

            @JsonProperty("items")
            Items items,

            @JsonProperty("numOfRows")
            Integer numOfRows,

            @JsonProperty("pageNo")
            Integer pageNo,

            @JsonProperty("totalCount")
            Integer totalCount
    ) {
        /** 도착 정보가 하나도 없는 경우 */
        public boolean isEmpty() {
            return items == null || items.item() == null || items.item().isEmpty();
        }
    }

    // ----------------------------------------------------------
    // 중첩 record: Items
    //  주의: 국토교통부 API는 결과가 1건이면 item을 Object로,
    //          2건 이상이면 Array로 반환하는 불일치가 있음.
    //          → application.yml의 DeserializationFeature 설정 또는
    //            ObjectMapper 커스터마이징으로 처리.
    // ----------------------------------------------------------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(

            @JsonProperty("item")
            List<BusArrivalItem> item  // 도착 정보 목록
    ) {}

    // ----------------------------------------------------------
    // 편의 메서드: 도착 정보 목록 직접 접근
    // ----------------------------------------------------------

    /**
     * 도착 정보 목록 반환.
     * 응답이 비어있거나 오류인 경우 빈 리스트 반환.
     */
    public List<BusArrivalItem> getArrivalItems() {
        if (response == null || response.body() == null || response.body().isEmpty()) {
            return List.of();
        }
        List<BusArrivalItem> items = response.body().items().item();
        return items != null ? items : List.of();
    }

    /**
     * API 응답 헤더의 성공 여부 확인.
     */
    public boolean isApiSuccess() {
        return response != null
                && response.header() != null
                && response.header().isSuccess();
    }
}
