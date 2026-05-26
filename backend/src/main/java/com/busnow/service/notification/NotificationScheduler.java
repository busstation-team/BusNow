package com.busnow.service.notification;

import com.busnow.dto.api.BusArrivalApiResponse;
import com.busnow.entity.NotificationSettings;
import com.busnow.repository.NotificationSettingsRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificationScheduler {

    private final NotificationSettingsRepository notificationSettingsRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;
    private final String arrivalUrl;

    public NotificationScheduler(
            NotificationSettingsRepository notificationSettingsRepository,
            @Qualifier("busArrivalRestClient") RestClient restClient,
            @Value("${external.api.service-key}") String serviceKey,
            @Value("${external.api.arrival-url}") String arrivalUrl
    ) {
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.restClient = restClient;
        this.serviceKey = serviceKey;
        this.arrivalUrl = arrivalUrl;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void checkArrivalNotifications() {
        log.info("[Scheduler] 도착 알림 체크 시작");
        List<NotificationSettings> activeSettings = notificationSettingsRepository.findAllActiveWithDetails();
        if (activeSettings.isEmpty()) return;

        Map<String, List<NotificationSettings>> groupedByStop = activeSettings.stream()
                .collect(Collectors.groupingBy(n -> n.getStop().getStopId()));

        groupedByStop.forEach((stopId, settingsForStop) -> {
            try {
                checkNotificationsForStop(stopId, settingsForStop);
            } catch (Exception e) {
                log.error("[Scheduler] stopId={} 처리 중 오류: {}", stopId, e.getMessage());
            }
        });
    }

    private void checkNotificationsForStop(String stopId, List<NotificationSettings> settingsForStop) {
        List<com.busnow.dto.api.BusArrivalItem> arrivalItems = fetchArrivalItemsRaw(stopId, "31190");
        if (arrivalItems == null || arrivalItems.isEmpty()) return;

        for (NotificationSettings setting : settingsForStop) {
            String targetRouteId = setting.getBusRoute().getRouteId();
            Integer alertTime = setting.getAlertTime();
            if (alertTime == null) continue;

            arrivalItems.stream()
                    .filter(item -> targetRouteId.equals(item.routeId()))
                    .filter(item -> item.arrTime() != null && item.arrTime() <= alertTime)
                    .findFirst()
                    .ifPresent(item -> triggerNotification(setting, item));
        }
    }

    private void triggerNotification(NotificationSettings setting, com.busnow.dto.api.BusArrivalItem item) {
        int remainMinutes = item.arrTime() != null ? item.arrTime() / 60 : 0;
        log.info("[ALERT] 🚌 알림 발송! 사용자={} | 정류소={} | 노선={} | {}분 후 도착",
                setting.getUser().getUsername(), setting.getStop().getStopName(),
                setting.getBusRoute().getRouteName(), remainMinutes);
    }

    private List<com.busnow.dto.api.BusArrivalItem> fetchArrivalItemsRaw(String stopId, String cityCode) {
        try {
            DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(arrivalUrl);
            factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

            java.net.URI uri = factory.builder()
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("cityCode", cityCode)
                    .queryParam("nodeId", stopId)
                    .queryParam("numOfRows", 10)
                    .queryParam("_type", "json")
                    .build();

            String rawJson = restClient.get().uri(uri).retrieve().body(String.class);
            BusArrivalApiResponse response = objectMapper.readValue(rawJson, BusArrivalApiResponse.class);
            return response.getArrivalItems();
        } catch (Exception e) {
            log.warn("[Scheduler] API 호출 실패: stopId={}, {}", stopId, e.getMessage());
            return List.of();
        }
    }
}
