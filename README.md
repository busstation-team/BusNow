# 🚌 BusNow (대중교통 실시간 알리미)

**"놓치기 쉬운 버스, 이제 5분 전에 미리 알려드립니다."**
공공 인프라 교통 데이터를 안정적으로 연동하고, 실시간 트래픽을 효율적으로 처리하는 데 집중한 웹 서비스입니다.

## 🌟 핵심 역량 및 주요 기능

* **공공 데이터 처리**: 공공데이터포털 대중교통 API를 활용한 실시간 도착 정보 수집 및 가공
* **안전한 사용자 인증**: Google, Naver, Kakao OAuth2 기반 간편 로그인 지원
* **무상태(Stateless) 보안**: JWT(Access/Refresh Token) 및 Redis를 활용한 토큰 제어
* **비동기 UI/UX**: React를 활용하여 새로고침 없는 빠르고 매끄러운 즐겨찾기 목록 업데이트
* **실시간 맞춤형 알림**: 사용자가 설정한 특정 버스의 정류장 도착 5분 전 자동 알림 발송

## 🛠 기술 스택

* **Backend**: Java 17, Spring Boot 3.x, Spring Security, OAuth2 Client, JWT
* **Database**: MySQL (3-NF 정규화 기반 구조), Redis (토큰 블랙리스트 관리)
* **Frontend**: React.js
* **API**: 공공데이터포털 (대중교통 실시간 API)

## 🏗 시스템 설계 (Architecture & ERD)

* **데이터베이스 정규화 (ERD)**: 공공 데이터 전체를 저장하지 않고, 사용자 알림 설정 및 즐겨찾기 메타 데이터 위주로 경량화하여 3-NF 정규화를 거친 구조를 설계했습니다.
* [ERD 이미지 삽입 위치: 여기에 캡처한 DB 구조 이미지를 넣어주세요]
* **3-Tier Architecture**: 클라이언트(React), 비즈니스 로직(Spring Boot), 데이터(MySQL/Redis)로 분리된 서버 아키텍처를 구축했습니다.
* [시스템 아키텍처 이미지 삽입 위치: 여기에 캡처한 구조 이미지를 넣어주세요]

## 🔥 트러블슈팅: JWT 로그아웃 및 토큰 무효화(Blacklist) 전략

* **문제 상황**: 로그아웃 시 클라이언트에서 토큰을 삭제하더라도, 탈취된 Access Token의 유효시간이 남아있다면 서버 접근이 가능한 보안 취약점 발생 가능성을 인지했습니다.
* **해결 1 (Refresh Token 관리)**: Redis를 도입하여 Refresh Token을 서버 측에서 안전하게 관리하고, 로그아웃 시 즉시 삭제하여 재발급을 원천 차단했습니다.
* **해결 2 (Access Token 블랙리스트)**: 탈취된 토큰의 즉각적인 무효화를 위해, Redis에 남은 유효시간만큼 해당 Access Token을 블랙리스트로 등록했습니다. 대중교통 알림 서비스 특성상 인프라급의 세밀한 세션 제어와 개인 정보 보안이 최우선이라고 판단하여 적용한 결과입니다.

## 💻 시작하기 (Getting Started)

* **Backend (Server)**: `server` 폴더 이동 후 `application.yml` 환경변수 세팅 및 `./gradlew bootRun` 실행
* **Frontend (Client)**: `client` 폴더 이동 후 `npm install` 및 `npm start` 실행
