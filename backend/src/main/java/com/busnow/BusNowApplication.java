package com.busnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BusNow 애플리케이션 진입점.
 *
 * @EnableScheduling: 도착 알림 백그라운드 스케줄러(5단계)를 위해 활성화.
 */
@SpringBootApplication
@EnableScheduling
public class BusNowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusNowApplication.class, args);
    }
}
