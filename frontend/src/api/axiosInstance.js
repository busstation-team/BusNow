/**
 * axiosInstance.js
 * BusNow 전역 Axios 인스턴스 + JWT 인터셉터
 *
 * ✅ 아키텍처:
 *   - Access Token: 메모리(React 상태)에서 관리 → 헤더에 자동 주입
 *   - Refresh Token: HttpOnly Cookie (서버가 Set-Cookie로 설정, JS 접근 불가)
 *   - 401 에러 발생 시: /api/auth/refresh 호출 → 새 AT 발급 → 원본 요청 재시도
 *   - 재발급도 실패하면: 로그아웃 처리 + 로그인 페이지 리다이렉트
 *
 * ✅ 재시도 무한루프 방지:
 *   - _retry 플래그: 한 번 재시도한 요청은 다시 재시도하지 않음
 *   - /auth/refresh 요청 자체가 401이면 → 로그아웃 (RT 만료)
 *
 * ✅ 동시 요청 처리:
 *   - isRefreshing 플래그 + failedQueue 패턴으로
 *     AT 재발급 중 발생한 다수의 요청을 큐에 쌓아두고
 *     재발급 완료 후 일괄 재시도 (Race Condition 방지)
 */

import axios from 'axios';

// ============================================================
// Access Token 메모리 저장소
// AuthContext에서 setAccessToken을 주입하여 사용
// ============================================================
let accessToken = null;          // 현재 유효한 Access Token
let isRefreshing = false;        // 토큰 재발급 진행 중 여부
let failedQueue = [];            // 재발급 대기 중인 요청 큐

/**
 * 현재 메모리에 저장된 Access Token 반환.
 * AuthContext에서 사용.
 */
export const getAccessToken = () => accessToken;

/**
 * Access Token을 메모리에 저장.
 * 로그인/재발급 성공 시 AuthContext에서 호출.
 */
export const setAccessToken = (token) => {
  accessToken = token;
};

/**
 * 대기 중인 요청 큐를 처리.
 * 재발급 성공 시 error=null, 새 토큰 전달 → 각 요청 resolve.
 * 재발급 실패 시 error 전달 → 각 요청 reject.
 */
const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  failedQueue = [];
};

// ============================================================
// Axios 인스턴스 생성
// ============================================================
const api = axios.create({
  baseURL: '/api',                        // vite.config.js 프록시 → localhost:8080/api
  timeout: 20000,                         // 20초 타임아웃 (공공 API 지연 대비)
  withCredentials: true,                  // ✅ HttpOnly Cookie(RT) 자동 전송 필수
  headers: {
    'Content-Type': 'application/json',
  },
});

// ============================================================
// 요청 인터셉터: 모든 요청에 Access Token 자동 주입
// ============================================================
api.interceptors.request.use(
  (config) => {
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    console.log(`[Axios Request] ${config.method.toUpperCase()} ${config.url}`, 
                `| Token: ${accessToken ? accessToken.substring(0, 10) + '...' : 'NONE'}`);
    return config;
  },
  (error) => Promise.reject(error)
);

// ============================================================
// 응답 인터셉터: 401 에러 시 AT 재발급 + 원본 요청 재시도
// ============================================================
api.interceptors.response.use(
  // 정상 응답: 그대로 반환
  (response) => response,

  // 에러 응답 처리
  async (error) => {
    const originalRequest = error.config;

    // 401 이외의 에러는 그대로 reject
    if (!error.response || error.response.status !== 401) {
      return Promise.reject(error);
    }

    // ✅ /auth/refresh 요청 자체가 401: RT 만료 → 강제 로그아웃
    if (originalRequest.url?.includes('/auth/refresh')) {
      handleForceLogout();
      return Promise.reject(error);
    }

    // ✅ 이미 재시도한 요청이면 중단 (무한루프 방지)
    if (originalRequest._retry) {
      return Promise.reject(error);
    }

    // ============================================================
    // 동시 401 요청 처리: 첫 번째 요청이 재발급 중일 때
    // 나머지 요청은 큐에 쌓아두고 재발급 완료 후 일괄 재시도
    // ============================================================
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((newToken) => {
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return api(originalRequest);
      }).catch((err) => Promise.reject(err));
    }

    // ============================================================
    // AT 재발급 시작
    // ============================================================
    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // HttpOnly Cookie의 RT를 서버가 자동으로 읽어서 새 AT 발급
      const response = await axios.post('/api/auth/refresh', {}, {
        withCredentials: true,  // Cookie 전송 필수
      });

      const newAccessToken = response.data.accessToken;
      setAccessToken(newAccessToken);

      // 대기 중인 요청들에게 새 토큰 전달
      processQueue(null, newAccessToken);

      // 원본 요청에 새 토큰 적용 후 재시도
      originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
      return api(originalRequest);

    } catch (refreshError) {
      // 재발급 실패: 대기 큐 전부 에러 처리 + 강제 로그아웃
      processQueue(refreshError, null);
      handleForceLogout();
      return Promise.reject(refreshError);

    } finally {
      isRefreshing = false;
    }
  }
);

// ============================================================
// 강제 로그아웃 처리
// ============================================================

/**
 * RT 만료/무효화로 인한 강제 로그아웃.
 * AT 메모리 초기화 + 로그인 페이지 리다이렉트.
 * window.dispatchEvent로 AuthContext에도 알림.
 */
const handleForceLogout = () => {
  setAccessToken(null);
  // AuthContext가 이 이벤트를 수신하여 상태 초기화
  window.dispatchEvent(new CustomEvent('auth:forceLogout'));
  // 로그인 페이지로 리다이렉트
  if (!window.location.pathname.includes('/login')) {
    window.location.href = '/login';
  }
};

export default api;
