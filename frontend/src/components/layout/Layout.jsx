/**
 * Layout.jsx
 * 전체 페이지 공통 레이아웃 래퍼
 *
 * - Header: 상단 고정
 * - main: 콘텐츠 영역 (헤더 높이 + 하단 탭 바 여백 자동 처리)
 * - BottomTabBar: 모바일 하단 고정
 */
import Header from './Header';
import BottomTabBar from './BottomTabBar';

export default function Layout({ children, className = '' }) {
  return (
    <div className="min-h-screen flex flex-col">
      {/* 상단 헤더 */}
      <Header />

      {/* 메인 콘텐츠 영역 */}
      <main className={`flex-1 pt-6 pb-24 w-full ${className}`}>
        {children}
      </main>

      {/* 모바일 하단 탭 바 */}
      <BottomTabBar />
    </div>
  );
}

/**
 * PageContainer: 페이지별 최대 너비 + 패딩 컨테이너
 * 각 페이지 내부에서 콘텐츠를 감싸는 용도
 */
export function PageContainer({ children, className = '' }) {
  return (
    <div className={`w-full max-w-2xl mx-auto py-6 ${className}`}>
      {children}
    </div>
  );
}
