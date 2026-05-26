import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const BusIconLg = () => (
  <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-white">
    <rect x="4" y="10" width="16" height="10" rx="2" ry="2"/>
    <path d="M6 10V6a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v4"/>
    <path d="M9 14h.01"/>
    <path d="M15 14h.01"/>
    <path d="M12 20v2"/>
    <path d="M8 20v2"/>
    <path d="M16 20v2"/>
  </svg>
);

const EyeIcon = ({ open }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    {open ? (
      <>
        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
        <circle cx="12" cy="12" r="3"/>
      </>
    ) : (
      <>
        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
        <line x1="1" y1="1" x2="23" y2="23"/>
      </>
    )}
  </svg>
);

const LoginPage = () => {
  const [form, setForm] = useState({ username: '', password: '' });
  const [showPw, setShowPw] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [serverError, setServerError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (serverError) setServerError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.username || !form.password) {
      setServerError('아이디와 비밀번호를 모두 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setServerError('');
    
    try {
      await login(form.username, form.password);
      setSuccessMsg('로그인 성공! 잠시 후 이동합니다.');
      setServerError('');
      setTimeout(() => navigate('/'), 400);
    } catch (err) {
      console.error('Login error:', err);
      // 서버에서 내려주는 에러 메시지가 있으면 표시, 없으면 기본 메시지
      const errMsg = err.response?.data?.message || '아이디 또는 비밀번호를 확인해주세요.';
      setServerError(errMsg);
      setSuccessMsg('');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex font-['Outfit'] selection:bg-slate-950 selection:text-white">
      {/* 브라우저 자동완성 파란 배경 제거를 위한 스타일 */}
      <style>
        {`
          input:-webkit-autofill,
          input:-webkit-autofill:hover, 
          input:-webkit-autofill:focus, 
          input:-webkit-autofill:active {
            -webkit-box-shadow: 0 0 0 30px white inset !important;
            -webkit-text-fill-color: #020617 !important;
          }
        `}
      </style>

      {/* ── 좌측 섹션 (네이비 정체성) ── */}
      <div className="hidden lg:flex lg:w-1/2 bg-slate-950 flex-col items-center justify-center p-20 pb-40 relative overflow-hidden text-center">
        <div className="absolute top-0 right-0 w-full h-full bg-[radial-gradient(circle_at_center,_rgba(37,99,235,0.08)_0%,_transparent_70%)]" />
        <div className="relative z-10">
          <div className="mb-12 flex flex-col items-center">
            <div className="w-20 h-20 bg-white/5 border border-white/10 rounded-[24px] flex items-center justify-center mb-8 shadow-2xl">
              <BusIconLg />
            </div>
            <h1 className="text-4xl font-black text-white tracking-tighter mb-2 italic">BusNow</h1>
            <p className="text-white/40 text-[10px] font-black uppercase tracking-[0.4em]">Real-time Bus Service</p>
          </div>
          <h2 className="text-6xl font-black text-white leading-[1.1] tracking-tighter mb-10">
            버스 도착 정보를<br />
            <span className="text-brand-main">가장 쉽고 빠르게</span>
          </h2>
          <div className="w-16 h-1.5 bg-brand-main rounded-full mx-auto" />
        </div>
      </div>

      {/* ── 우측 섹션 (최종 개조 레이아웃) ── */}
      <div className="flex-1 flex items-center justify-center p-12 bg-slate-200">
        <div className="w-full max-w-[400px]">
          
          {/* 제목 섹션 - 폰트 확대 반영 */}
          <div className="text-center lg:text-left">
            <h2 className="text-5xl font-black text-slate-950 tracking-tight leading-tight">
              환영합니다
            </h2>
            <p className="text-slate-400 text-lg mt-3 font-medium">당신의 스마트한 여정을 시작하세요.</p>
          </div>

          {/* Spacer */}
          <div className="h-4" />

          {/* 폼 - 절대 겹칠 수 없는 Flex 구조 */}
          <form onSubmit={handleSubmit} className="flex flex-col gap-y-4">
            
            {/* Username */}
            <div className="flex flex-col gap-y-1.5">
              <label className="text-[11px] font-black text-slate-400 uppercase tracking-[0.2em] ml-1">username</label>
              <div 
                className="flex items-center w-full h-14 bg-white border border-slate-100 rounded-2xl focus-within:border-slate-950 focus-within:ring-4 focus-within:ring-slate-950/5 transition-all shadow-sm group"
                style={{ paddingLeft: '22px', paddingRight: '22px', gap: '12px' }}
              >
                <div className="text-slate-300 group-focus-within:text-slate-950 transition-colors">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                </div>
                <input
                  type="text"
                  name="username"
                  value={form.username}
                  onChange={handleChange}
                  placeholder="아이디를 입력하세요"
                  className="flex-1 h-full bg-transparent border-none outline-none text-slate-950 text-base font-bold placeholder:text-slate-200"
                  disabled={isLoading}
                />
              </div>
            </div>

            {/* Password */}
            <div className="flex flex-col gap-y-1.5">
              <label className="text-[11px] font-black text-slate-400 uppercase tracking-[0.2em] ml-1">password</label>
              <div 
                className="flex items-center w-full h-14 bg-white border border-slate-100 rounded-2xl focus-within:border-slate-950 focus-within:ring-4 focus-within:ring-slate-950/5 transition-all shadow-sm group"
                style={{ paddingLeft: '22px', paddingRight: '22px', gap: '12px' }}
              >
                <div className="text-slate-300 group-focus-within:text-slate-950 transition-colors">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </div>
                <input
                  type={showPw ? 'text' : 'password'}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="비밀번호를 입력하세요"
                  className="flex-1 h-full bg-transparent border-none outline-none text-slate-950 text-base font-bold placeholder:text-slate-200"
                  disabled={isLoading}
                />
                <button
                  type="button"
                  onClick={() => setShowPw(!showPw)}
                  className="text-slate-300 hover:text-slate-950 transition-colors ml-2"
                >
                  <EyeIcon open={showPw} />
                </button>
              </div>
            </div>

            {/* 에러/성공 메시지 */}
            {(serverError || successMsg) && (
              <div className="min-h-[20px]">
                {successMsg && <p className="text-xs font-black text-green-600 ml-1">✓ {successMsg}</p>}
                {serverError && <p className="text-xs font-black text-red-500 ml-1">! {serverError}</p>}
              </div>
            )}

            {/* 로그인 버튼 - 심플 호버 효과 */}
            <div className="mt-2">
              <button
                type="submit"
                disabled={isLoading}
                className="w-full h-14 bg-slate-950 hover:bg-slate-800 hover:scale-[1.01] text-white font-black text-lg transition-all active:scale-[0.98] disabled:opacity-50 rounded-2xl"
              >
                {isLoading ? '처리 중...' : '로그인'}
              </button>
            </div>
          </form>

          {/* 하단 푸터 */}
          <div className="mt-10 pt-6 border-t border-slate-300 flex flex-col items-center gap-4">
            <span className="text-slate-400 text-xs font-medium">처음 방문하셨나요?</span>
            <Link
              to="/register"
              className="text-slate-950 text-xs font-black border-b border-slate-950 pb-0.5 hover:text-brand-main hover:border-brand-main transition-all"
            >
              새 계정 만들기
            </Link>
          </div>
          <p className="mt-8 text-center text-[8px] font-bold text-slate-400 tracking-[0.3em] uppercase opacity-40">
            © BusNow System
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
