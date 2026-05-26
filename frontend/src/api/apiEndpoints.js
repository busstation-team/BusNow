/**
 * apiEndpoints.js
 * 모든 API 엔드포인트 URL을 상수로 중앙 관리
 */

export const API = {
  AUTH: {
    LOGIN:    '/auth/login',
    REGISTER: '/auth/register',
    REFRESH:  '/auth/refresh',
    SIGNOUT:  '/auth/signout',
  },
  STOPS: {
    SEARCH: '/stops/search',         // ?keyword=
  },
  BUS: {
    ARRIVAL: (stopId) => `/bus/arrival/${stopId}`,   // ?cityCode=
    BATCH:   '/bus/arrival/batch',
  },
  FAVORITES: {
    LIST:       '/favorites',
    ADD:        '/favorites',
    ARRIVAL:    '/favorites/arrival',              // ?cityCode= (메인 폴링)
    ALIAS:      (id) => `/favorites/${id}/alias`,
    DELETE:     (id) => `/favorites/${id}`,
    DELETE_STOP:(stopId) => `/favorites/stop/${stopId}`,
  },
  NOTIFICATIONS: {
    LIST:         '/notifications',
    ADD:          '/notifications',
    UPDATE_TIME:  (id) => `/notifications/${id}/alert-time`,
    TOGGLE:       (id) => `/notifications/${id}/toggle`,
    DELETE:       (id) => `/notifications/${id}`,
    DELETE_BY_ROUTE: (stopId, routeId) => `/notifications/route?stopId=${stopId}&routeId=${routeId}`,
  },
};
