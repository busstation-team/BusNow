/**
 * App.jsx
 * 라우팅 + 전역 Provider 설정
 *
 * ✅ Protected Route:
 *    로그인 안 된 상태로 보호 경로 접근 시 /login으로 리다이렉트.
 *    isLoading 중에는 스켈레톤 스피너 표시 (AT 복원 완료 전 깜박임 방지).
 */
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastContainer } from './components/common/Toast';
import { useToast, ToastProvider } from './hooks/useToast';
import Layout from './components/layout/Layout';
import GlobalNotificationHandler from './components/common/GlobalNotificationHandler';

// 페이지 컴포넌트 (2단계 이후 구현 - 플레이스홀더)
import LoginPage    from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import MainPage     from './pages/MainPage';
import SearchPage   from './pages/SearchPage';
import FavoritesPage    from './pages/FavoritesPage';
import NotificationsPage from './pages/NotificationsPage';

// ============================================================
// Protected Route 래퍼
// ============================================================
function ProtectedRoute({ children }) {
  const { user, isLoading } = useAuth();

  // 토큰 복구 중일 때는 로딩 스피너 표시
  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-slate-50">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-brand-main mb-4"></div>
        <p className="text-slate-400 text-sm font-medium">로그인 정보 복원 중...</p>
      </div>
    );
  }

  // 복구가 끝났는데도 user가 없으면 로그인 페이지로 리다이렉트
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

// ============================================================
// 앱 내부 (AuthProvider 안에서 useToast 사용 가능)
// ============================================================
function AppInner() {
  const { toasts, dismissToast } = useToast();

  return (
    <>
      <Routes>
        {/* 공개 라우트 */}
        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/"         element={<Navigate to="/main" replace />} />

        {/* 보호 라우트 */}
        <Route path="/main" element={
          <ProtectedRoute>
            <Layout><MainPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/search" element={
          <ProtectedRoute>
            <Layout><SearchPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/favorites" element={
          <ProtectedRoute>
            <Layout><FavoritesPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/notifications" element={
          <ProtectedRoute>
            <Layout><NotificationsPage /></Layout>
          </ProtectedRoute>
        } />

        {/* 404 */}
        <Route path="*" element={<Navigate to="/main" replace />} />
      </Routes>

      {/* 전역 알림 핸들러 (백그라운드 감시) */}
      <GlobalNotificationHandler />

      {/* 전역 토스트 */}
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <AppInner />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
