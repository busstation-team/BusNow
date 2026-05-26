/**
 * BottomTabBar.jsx
 * 모바일 전용 하단 탭 내비게이션
 */
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const HomeIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
  </svg>
);

const SearchIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
  </svg>
);

const StarIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
  </svg>
);

const BellIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>
  </svg>
);

const TABS = [
  { to: '/main',          label: '홈',     Icon: HomeIcon },
  { to: '/search',        label: '검색',   Icon: SearchIcon },
  { to: '/favorites',     label: '즐겨찾기',Icon: StarIcon },
  { to: '/notifications', label: '알림',   Icon: BellIcon },
];

export default function BottomTabBar() {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) return null;

  return (
    <nav className="fixed bottom-8 left-6 right-6 z-50">
      <div 
        className="glass-dark rounded-[2rem] flex items-center justify-around shadow-2xl border border-white/10 overflow-hidden max-w-lg mx-auto"
        style={{ paddingTop: '12px', paddingBottom: '8px', paddingLeft: '16px', paddingRight: '16px' }}
      >
        {TABS.map(({ to, label, Icon }) => {
          const isActive = location.pathname === to;
          return (
            <Link
              key={to}
              to={to}
              className={`flex flex-col items-center justify-center w-14 h-14 rounded-2xl transition-all duration-300
                ${isActive 
                  ? 'bg-brand-main text-white shadow-lg scale-110 -translate-y-1' 
                  : 'text-white/40 hover:text-white/70'
                }`}
            >
              <div className={isActive ? 'mb-0.5' : ''}>
                <Icon />
              </div>
              {isActive && (
                <span className="text-[9px] font-black uppercase tracking-tighter animate-fade-in">
                  {label}
                </span>
              )}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
