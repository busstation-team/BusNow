package com.busnow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring Boot 3.2+ RestClient 설정.
 *
 *  RestClient vs RestTemplate:
 *    - RestTemplate: Spring 5.0부터 유지보수 모드 (새 기능 추가 없음).
 *    - RestClient: Spring 6.1(Boot 3.2)에서 도입된 동기식 HTTP 클라이언트.
 *      fluent API, 람다 기반 설정, WebClient와 유사한 UX.
 *
 *  국토교통부 API 전용 RestClient:
 *    - baseUrl: application.yml의 external.api.base-url 주입.
 *    - Accept: application/json → `_type=json` 파라미터와 함께 JSON 응답 강제.
 *    - 타임아웃: 연결 3초, 읽기 5초 (외부 API 응답 지연 대응).
 */
@Configuration
public class RestClientConfig {


    @Value("${external.api.timeout}")
    private int timeoutMs;

    /**
     * 국토교통부 버스 도착 정보 API 전용 RestClient 빈.
     *
     * @Bean("busArrivalRestClient"): 다른 외부 API RestClient와 구분하기 위해 이름 지정.
     *   서비스 계층에서 @Qualifier("busArrivalRestClient")로 주입.
     */
    @Bean("busArrivalRestClient")
    public RestClient busArrivalRestClient() {
        // SimpleClientHttpRequestFactory: 기본 JDK HttpURLConnection 기반 팩토리.
        // 운영 환경에서는 Apache HttpClient 또는 OkHttp로 교체 권장.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);    // 연결 타임아웃: 3초
        factory.setReadTimeout(timeoutMs);  // 읽기 타임아웃: application.yml 설정값 (5초)

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // 응답 에러 처리: 4xx/5xx 시 기본 예외(RestClientResponseException) 발생
                // 커스텀 핸들러가 필요하면 .defaultStatusHandler() 추가
                .build();
    }
}
