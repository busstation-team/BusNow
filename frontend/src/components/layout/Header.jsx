/**
 * Header.jsx
 * 상단 글로벌 헤더 컴포넌트
 *
 * - PC/태블릿: 상단 고정 헤더 (글래스모피즘 효과)
 * - 모바일: 로고 + 현재 페이지 타이틀 + 우측 액션 버튼
 */
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

// SVG 아이콘 (인라인, 의존성 최소화)
const BusIcon = () => (
  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="1" y="3" width="15" height="13" rx="2"/>
    <path d="M16 8h4l3 3v3h-7V8z"/>
    <circle cx="5.5" cy="18.5" r="2.5"/>
    <circle cx="18.5" cy="18.5" r="2.5"/>
  </svg>
);

const BellIcon = ({ active }) => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill={active ? '#FACC15' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
  </svg>
);

const UserIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
    <circle cx="12" cy="7" r="4"/>
  </svg>
);

const PAGE_TITLES = {
  '/':             '실시간 버스',
  '/main':         '실시간 버스',
  '/search':       '정류소 검색',
  '/favorites':    '즐겨찾기',
  '/notifications':'알림 설정',
  '/login':        '로그인',
  '/register':     '회원가입',
};

export default function Header() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const pageTitle = PAGE_TITLES[location.pathname] ?? 'BusNow';

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header className="glass-dark sticky top-0 left-0 right-0 z-50 h-16 flex items-center">
      <div className="app-container w-full h-full flex items-center justify-between !py-0">

        {/* ── 로고 ── */}
        <Link
          to={user ? '/main' : '/login'}
          className="flex items-center gap-3 text-white group"
        >
          <div className="w-10 h-10 bg-white/10 rounded-xl flex items-center justify-center group-hover:bg-brand-main transition-all duration-300 shadow-inner">
            <BusIcon />
          </div>
          <div className="flex flex-col leading-tight">
            <span className="font-black text-xl tracking-tighter">BusNow</span>
            <span className="text-[10px] font-bold text-white/40 uppercase tracking-widest hidden sm:block">
              Real-time Bus
            </span>
          </div>
        </Link>

        {/* ── PC 네비게이션 (고정 너비 알약 디자인) ── */}
        {user && (
          <nav className="hidden md:flex items-center gap-4">
            {[
              { to: '/main',          label: '홈' },
              { to: '/search',        label: '검색' },
              { to: '/favorites',     label: '즐겨찾기' },
              { to: '/notifications', label: '알림' },
            ].map(({ to, label }) => (
              <Link
                key={to}
                to={to}
                className={`min-w-[110px] h-12 flex items-center justify-center px-6 rounded-full text-[16px] font-black transition-all duration-300
                  ${location.pathname === to
                    ? 'bg-white text-brand-main shadow-[0_12px_30px_-10px_rgba(0,0,0,0.4)]'
                    : 'text-white/50 hover:text-white hover:bg-white/10'
                  }`}
              >
                {label}
              </Link>
            ))}
          </nav>
        )}

        {/* ── 우측 액션 버튼 ── */}
        <div className="flex items-center gap-3">
          {user ? (
            <div className="relative group">
              {/* 사용자 프로필 버튼 (둥근 디자인 복구) */}
              <button className="flex items-center gap-4 pl-1.5 pr-4 py-1.5 bg-white/5 hover:bg-white/15 border border-white/10 rounded-full transition-all duration-300 shadow-xl group-hover:border-white/20">
                <div className="w-10 h-10 bg-gradient-to-tr from-brand-main to-brand-point rounded-full flex items-center justify-center shadow-lg border border-white/20">
                  <span className="text-sm font-black text-white">
                    {user.username?.charAt(0).toUpperCase()}
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-base font-black text-white tracking-tight hidden sm:block">
                    {user.username}
                  </span>
                  <svg className="w-4 h-4 text-white/40 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M19 9l-7 7-7-7" />
                  </svg>
                </div>
              </button>

              {/* 드롭다운 메뉴 (글자 잘림 해결 및 간격 최적화) */}
              <div 
                className="absolute right-0 top-full mt-4 w-40 bg-slate-900 border border-white/10 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-500 origin-top-right translate-y-2 group-hover:translate-y-0 shadow-[0_20px_40px_rgba(0,0,0,0.6)] overflow-hidden flex flex-col"
                style={{ borderRadius: '12px', height: '90px' }}
              >
                {/* 상단: 이름 영역 (잘림 방지 pb-1 추가) */}
                <div className="flex-1 flex items-center justify-center border-b border-white/5 px-4">
                  <span 
                    className="font-black text-white truncate pb-1"
                    style={{ fontSize: '14px' }}
                  >
                    {user.username}
                  </span>
                </div>

                {/* 하단: 로그아웃 버튼 영역 */}
                <div className="flex-1 flex items-center justify-center p-1">
                  <button
                    onClick={handleLogout}
                    className="w-full h-full flex items-center justify-center gap-2 text-red-400 hover:text-red-300 hover:bg-white/5 rounded-lg transition-all font-black"
                    style={{ fontSize: '14px' }}
                  >
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                    로그아웃
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <Link to="/login" className="btn-primary !py-2 !px-5 text-sm">
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
