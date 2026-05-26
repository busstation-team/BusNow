package com.busnow.service.bus;

import com.busnow.dto.api.ArrivalInfoResponse;
import com.busnow.dto.api.BusArrivalItem;
import com.busnow.dto.api.StopArrivalResponse;
import com.busnow.dto.api.StopSearchResponse;
import com.busnow.entity.Stops;

import com.busnow.repository.StopsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BusArrivalService {

    private final RestClient restClient;
    private final StopsRepository stopsRepository;
    private final ObjectMapper objectMapper;
    private final String serviceKey;
    private final String arrivalUrl;
    private final String stopSearchUrl;

    public BusArrivalService(
            @Qualifier("busArrivalRestClient") RestClient restClient,
            StopsRepository stopsRepository,
            ObjectMapper objectMapper,
            @Value("${external.api.service-key}") String serviceKey,
            @Value("${external.api.arrival-url}") String arrivalUrl,
            @Value("${external.api.stop-search-url}") String stopSearchUrl
    ) {
        this.restClient = restClient;
        this.stopsRepository = stopsRepository;
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
        this.arrivalUrl = arrivalUrl;
        this.stopSearchUrl = stopSearchUrl;
    }

    public StopArrivalResponse getArrivalInfo(String stopId, String cityCode) {
        try {
            String responseBody = callArrivalApi(stopId, cityCode);
            log.debug("[BusArrival] Raw Response: {}", responseBody); // 실제 응답 확인용 로그 추가

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode bodyNode = root.path("response").path("body");
            JsonNode itemsNode = bodyNode.path("items").path("item");

            List<BusArrivalItem> arrivals = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode node : itemsNode) {
                    arrivals.add(objectMapper.treeToValue(node, BusArrivalItem.class));
                }
            } else if (!itemsNode.isMissingNode() && !itemsNode.isNull() && !itemsNode.asText().isEmpty()) {
                // 단일 항목인 경우
                arrivals.add(objectMapper.treeToValue(itemsNode, BusArrivalItem.class));
            }

            List<ArrivalInfoResponse> responseItems = arrivals.stream()
                    .map(ArrivalInfoResponse::from)
                    .toList();

            // DB에서 정류소 이름 조회 (없거나 '알 수 없는 정류소'인 경우 API 응답에서 추출)
            String stopName = stopsRepository.findById(stopId)
                    .map(Stops::getStopName)
                    .filter(name -> !"알 수 없는 정류소".equals(name)) // '알 수 없는 정류소'로 저장된 경우 필터링
                    .orElseGet(() -> arrivals.stream()
                            .findFirst()
                            .map(BusArrivalItem::nodeName)
                            .orElse("알 수 없는 정류소"));

            return new StopArrivalResponse(
                    stopId,
                    stopName,
                    responseItems,
                    responseItems.size(),
                    false,
                    null
            );
        } catch (Exception e) {
            log.error("[BusArrival] API 호출 또는 파싱 오류: {}", e.getMessage());
            return new StopArrivalResponse(stopId, "오류", Collections.emptyList(), 0, true, "도착 정보를 가져올 수 없습니다.");
        }
    }

    /**
     * 여러 정류소의 도착 정보를 일괄 조회 (MainPage 폴링용)
     * 병렬 스트림(parallelStream)을 사용하여 API 호출 속도를 비약적으로 향상시킵니다.
     */
    public List<StopArrivalResponse> getArrivalInfoBatch(List<String> stopIds, String cityCode) {
        if (stopIds == null || stopIds.isEmpty()) return Collections.emptyList();
        
        log.info("[Batch] {}개 정류소 정보 병렬 조회 시작", stopIds.size());
        
        return stopIds.parallelStream()
                .map(id -> getArrivalInfo(id, cityCode))
                .toList();
    }

    public List<StopSearchResponse> searchStopsViaApi(String keyword, Integer cityCode) {
        int targetCityCode = (cityCode != null) ? cityCode : 31190;
        try {
            //  1. 검색어만 수동으로 인코딩
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

            //  2. 인증키 중복 인코딩 방지를 위해 EncodingMode.NONE 설정
            DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(stopSearchUrl);
            factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

            java.net.URI uri = factory.builder()
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("cityCode", targetCityCode)
                    .queryParam("nodeNm", encodedKeyword)
                    .queryParam("_type", "json")
                    .queryParam("numOfRows", 50)
                    .build();

            log.info("[API Request] Full URL: {}", uri);

            String responseBody = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");

            List<StopSearchResponse> results = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode node : itemsNode) {
                    results.add(mapToStopSearchResponse(node, targetCityCode));
                }
            } else if (!itemsNode.isMissingNode() && !itemsNode.isNull()) {
                results.add(mapToStopSearchResponse(itemsNode, targetCityCode));
            }
            return results;
        } catch (Exception e) {
            log.error("[API Error] 정류소 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private StopSearchResponse mapToStopSearchResponse(JsonNode node, int cityCode) {
        return new StopSearchResponse(
                node.path("nodeid").asText(),
                node.path("nodenm").asText(),
                node.path("nodeno").asText(),
                cityCode
        );
    }

    private String callArrivalApi(String stopId, String cityCode) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(arrivalUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        java.net.URI uri = factory.builder()
                .queryParam("serviceKey", serviceKey)
                .queryParam("cityCode", cityCode) // 대소문자 수정
                .queryParam("nodeId", stopId)     // 대소문자 수정
                .queryParam("numOfRows", 15)
                .queryParam("_type", "json")
                .build();

        log.debug("[Arrival API Request] URL: {}", uri);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }

}
