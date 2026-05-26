-- ============================================================
-- BusNow DB 초기화 DDL
-- Database: busnow_db
-- Charset: utf8mb4 (이모지 포함 완전한 유니코드 지원)
-- ============================================================

CREATE DATABASE IF NOT EXISTS busnow_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE busnow_db;

-- ------------------------------------------------------------
-- 1. Users 테이블
--    사용자 계정, JWT Refresh Token 저장
-- ------------------------------------------------------------
CREATE TABLE Users (
    User_id       INT AUTO_INCREMENT PRIMARY KEY,
    Username      VARCHAR(50)  NOT NULL UNIQUE     COMMENT '로그인 아이디',
    Password      VARCHAR(255) NOT NULL             COMMENT 'BCrypt 암호화 비밀번호',
    Email         VARCHAR(100) NOT NULL UNIQUE      COMMENT '이메일',
    Role          VARCHAR(20)  DEFAULT 'USER'       COMMENT '권한: USER | ADMIN',
    Refresh_token TEXT                              COMMENT 'JWT Refresh Token',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 2. Stops 테이블
--    국토교통부 API 정류장 마스터 데이터
-- ------------------------------------------------------------
CREATE TABLE Stops (
    Stop_id   VARCHAR(20)  PRIMARY KEY             COMMENT '국토부 정류장 고유 ID',
    Stop_name VARCHAR(100) NOT NULL                COMMENT '정류장 명칭'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 3. Bus_Routes 테이블
--    버스 노선 마스터 데이터
-- ------------------------------------------------------------
CREATE TABLE Bus_Routes (
    Route_id   VARCHAR(30) PRIMARY KEY             COMMENT '국토부 노선 고유 ID',
    Route_name VARCHAR(50) NOT NULL                COMMENT '노선 번호 또는 이름',
    Route_type VARCHAR(20)                         COMMENT '노선 유형: 일반/좌석/마을버스 등'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 4. Favorite_Stops 테이블
--    사용자별 즐겨찾기 정류장 (별칭 포함)
-- ------------------------------------------------------------
CREATE TABLE Favorite_Stops (
    Favorite_id INT AUTO_INCREMENT PRIMARY KEY,
    User_id     INT          NOT NULL,
    Stop_id     VARCHAR(20)  NOT NULL,
    Alias       VARCHAR(50)                        COMMENT '사용자 지정 별칭',
    Created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorite_user FOREIGN KEY (User_id)  REFERENCES Users(User_id)  ON DELETE CASCADE,
    CONSTRAINT fk_favorite_stop FOREIGN KEY (Stop_id)  REFERENCES Stops(Stop_id)  ON DELETE CASCADE,
    CONSTRAINT uq_favorite       UNIQUE (User_id, Stop_id)   COMMENT '동일 정류장 중복 즐겨찾기 방지'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 5. Notification_Settings 테이블
--    버스 도착 알림 설정 (정류장 + 노선 + 알림 시간)
-- ------------------------------------------------------------
CREATE TABLE Notification_Settings (
    Notification_id INT AUTO_INCREMENT PRIMARY KEY,
    User_id         INT          NOT NULL,
    Stop_id         VARCHAR(20)  NOT NULL,
    Route_id        VARCHAR(30)  NOT NULL,
    Alert_time      INT                            COMMENT '알림 기준 시간(초). ex) 180 = 3분 전',
    Is_active       BOOLEAN      DEFAULT TRUE       COMMENT '알림 활성화 여부',
    CONSTRAINT fk_notification_user  FOREIGN KEY (User_id)  REFERENCES Users(User_id)  ON DELETE CASCADE,
    CONSTRAINT fk_notification_stop  FOREIGN KEY (Stop_id)  REFERENCES Stops(Stop_id)  ON DELETE CASCADE,
    CONSTRAINT fk_notification_route FOREIGN KEY (Route_id) REFERENCES Bus_Routes(Route_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
