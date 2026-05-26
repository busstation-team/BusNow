/**
 * AuthContext.jsx
 * JWT 인증 상태 전역 관리 (Context API)
 *
 * ✅ 상태 전략:
 *   - accessToken: 메모리(useState) 저장 → XSS로 탈취 불가
 *   - user 정보: localStorage에 직렬화 저장 (민감 정보 제외, 새로고침 대응)
 *   - refreshToken: HttpOnly Cookie (서버가 관리, JS 접근 불가)
 *
 * ✅ 앱 시작 시 자동 복원:
 *   localStorage의 user 정보가 있으면 AT 재발급 시도 (/auth/refresh)
 *   → 성공: 로그인 상태 복원 / 실패: 로그아웃 처리
 */
import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { setAccessToken, getAccessToken } from '../api/axiosInstance';
import api from '../api/axiosInstance';
import { API } from '../api/apiEndpoints';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // user: { userId, username, role } (민감 정보 미포함)
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('busnow_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [isLoading, setIsLoading] = useState(true);  // 초기 복원 중 여부

  // ============================================================
  // 앱 시작 시 AT 자동 복원 (새로고침 대응)
  // ============================================================
  useEffect(() => {
    const restoreAuth = async () => {
      console.log('[AuthContext] 인증 복구 시도...');
      const stored = localStorage.getItem('busnow_user');
      if (!stored) {
        console.log('[AuthContext] 저장된 유저 정보 없음');
        setIsLoading(false);
        return;
      }

      try {
        console.log('[AuthContext] 토큰 재발급 요청 중...');
        const res = await axios.post(API.AUTH.REFRESH, {}, {
          baseURL: '/api',
          withCredentials: true,
        });
        
        console.log('[AuthContext] 재발급 성공:', res.data.username);
        setAccessToken(res.data.accessToken);
        setUser({
          userId:   res.data.userId,
          username: res.data.username,
          role:     res.data.role,
        });
      } catch (err) {
        const errorMsg = err.response?.data?.message || err.message;
        console.error('[AuthContext] 인증 복구 실패:', err.response?.status, errorMsg);
        // RT 만료 → 로그아웃 처리
        localStorage.removeItem('busnow_user');
        setUser(null);
        setAccessToken(null);
      } finally {
        setIsLoading(false);
      }
    };

    restoreAuth();
  }, []);

  // ============================================================
  // 강제 로그아웃 이벤트 수신 (axiosInstance에서 발행)
  // ============================================================
  useEffect(() => {
    const handleForceLogout = () => {
      setUser(null);
      setAccessToken(null);
      localStorage.removeItem('busnow_user');
    };
    window.addEventListener('auth:forceLogout', handleForceLogout);
    return () => window.removeEventListener('auth:forceLogout', handleForceLogout);
  }, []);

  // ============================================================
  // 로그인
  // ============================================================
  const login = useCallback(async (username, password) => {
    console.log('[AuthContext] 로그인 시도:', username);
    const res = await api.post(API.AUTH.LOGIN, { username, password });
    const { accessToken, userId, username: uname, role } = res.data;

    console.log('[AuthContext] 로그인 성공, 토큰 및 유저 정보 저장 중...');
    setAccessToken(accessToken);
    const userData = { userId, username: uname, role };
    setUser(userData);
    localStorage.setItem('busnow_user', JSON.stringify(userData));
    console.log('[AuthContext] localStorage 저장 완료');

    return res.data;
  }, []);

  const logout = useCallback(async () => {
    console.log('[AuthContext] 로그아웃 진행...');
    try {
      await api.post(API.AUTH.SIGNOUT);
    } catch {
    } finally {
      setUser(null);
      setAccessToken(null);
      localStorage.removeItem('busnow_user');
      console.log('[AuthContext] 로그아웃 완료, localStorage 초기화');
    }
  }, []);

  // ============================================================
  // 회원가입
  // ============================================================
  const register = useCallback(async (username, password, email) => {
    const res = await api.post(API.AUTH.REGISTER, { username, password, email });
    return res.data;
  }, []);

  const value = {
    user,
    isLoading,
    isAuthenticated: !!user,
    login,
    logout,
    register,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * useAuth 커스텀 훅
 * AuthContext 없이 사용하면 명확한 에러 발생
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.');
  }
  return context;
}
