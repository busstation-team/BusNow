import React, { useState, useEffect, useCallback } from 'react';
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

// 비밀번호 강도 계산
function getPasswordStrength(pw) {
  if (!pw) return { level: 0, label: '', color: '' };
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;

  if (score <= 1) return { level: 1, label: '취약', color: '#f87171' };
  if (score <= 3) return { level: 2, label: '보통', color: '#fbbf24' };
  return { level: 3, label: '강력', color: '#4ade80' };
}

const RegisterPage = () => {
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [showPw, setShowPw] = useState(false);
  const [showCpw, setShowCpw] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [serverError, setServerError] = useState('');
  const [errors, setErrors] = useState({});

  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) navigate('/');
  }, [isAuthenticated, navigate]);

  const strength = getPasswordStrength(form.password);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors(prev => ({ ...prev, [name]: '' }));
    setServerError('');
  };

  const validate = () => {
    const errs = {};
    if (!form.username.trim()) errs.username = '아이디를 입력해주세요.';
    if (!form.email.trim()) errs.email = '이메일을 입력해주세요.';
    else if (!/\S+@\S+\.\S+/.test(form.email)) errs.email = '올바른 이메일 형식이 아닙니다.';
    if (!form.password) errs.password = '비밀번호를 입력해주세요.';
    else if (form.password.length < 8) errs.password = '비밀번호는 최소 8자 이상이어야 합니다.';
    if (form.password !== form.confirmPassword) errs.confirmPassword = '비밀번호가 일치하지 않습니다.';
    
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsLoading(true);
    setServerError('');
    
    try {
      await register(form.username.trim(), form.password, form.email.trim());
      navigate('/login', { state: { message: '회원가입이 완료되었습니다. 로그인해주세요.' } });
    } catch (err) {
      setServerError(err.response?.data?.message || '회원가입 실패');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex font-['Outfit'] selection:bg-slate-950 selection:text-white">
      {/* 자동완성 파란 배경 제거 */}
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

      {/* ── 좌측 섹션 (로그인과 동일 컨셉) ── */}
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

      {/* ── 우측 섹션 (로그인과 동일 컨셉) ── */}
      <div className="flex-1 flex items-center justify-center p-12 bg-slate-200 overflow-y-auto">
        <div className="w-full max-w-[440px] py-12">
          
          <div className="text-center lg:text-left">
            <h2 className="text-5xl font-black text-slate-950 tracking-tight leading-tight">
              계정 만들기
            </h2>
            <p className="text-slate-400 text-lg mt-3 font-medium">지금 바로 무료로 시작하세요.</p>
          </div>

          <div className="h-4" />

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
              {errors.username && <p className="text-[10px] font-bold text-red-500 ml-2">{errors.username}</p>}
            </div>

            {/* Email */}
            <div className="flex flex-col gap-y-1.5">
              <label className="text-[11px] font-black text-slate-400 uppercase tracking-[0.2em] ml-1">email address</label>
              <div 
                className="flex items-center w-full h-14 bg-white border border-slate-100 rounded-2xl focus-within:border-slate-950 focus-within:ring-4 focus-within:ring-slate-950/5 transition-all shadow-sm group"
                style={{ paddingLeft: '22px', paddingRight: '22px', gap: '12px' }}
              >
                <div className="text-slate-300 group-focus-within:text-slate-950 transition-colors">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                </div>
                <input
                  type="email"
                  name="email"
                  value={form.email}
                  onChange={handleChange}
                  placeholder="이메일을 입력하세요"
                  className="flex-1 h-full bg-transparent border-none outline-none text-slate-950 text-base font-bold placeholder:text-slate-200"
                  disabled={isLoading}
                />
              </div>
              {errors.email && <p className="text-[10px] font-bold text-red-500 ml-2">{errors.email}</p>}
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
                  placeholder="최소 8자 이상"
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
              {/* 비밀번호 강도 인디케이터 (미니멀) */}
              {form.password && (
                <div className="flex items-center gap-2 ml-2 mt-0.5">
                  <div className="flex gap-1">
                    {[1, 2, 3].map(i => (
                      <div key={i} className="h-1 w-8 rounded-full transition-all duration-300"
                           style={{ background: i <= strength.level ? strength.color : '#e2e8f0' }} />
                    ))}
                  </div>
                  <span className="text-[10px] font-black uppercase tracking-widest" style={{ color: strength.color }}>
                    {strength.label}
                  </span>
                </div>
              )}
              {errors.password && <p className="text-[10px] font-bold text-red-500 ml-2">{errors.password}</p>}
            </div>

            {/* Confirm Password */}
            <div className="flex flex-col gap-y-1.5">
              <label className="text-[11px] font-black text-slate-400 uppercase tracking-[0.2em] ml-1">confirm password</label>
              <div 
                className="flex items-center w-full h-14 bg-white border border-slate-100 rounded-2xl focus-within:border-slate-950 focus-within:ring-4 focus-within:ring-slate-950/5 transition-all shadow-sm group"
                style={{ paddingLeft: '22px', paddingRight: '22px', gap: '12px' }}
              >
                <div className="text-slate-300 group-focus-within:text-slate-950 transition-colors">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                </div>
                <input
                  type={showCpw ? 'text' : 'password'}
                  name="confirmPassword"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  placeholder="비밀번호를 다시 입력하세요"
                  className="flex-1 h-full bg-transparent border-none outline-none text-slate-950 text-base font-bold placeholder:text-slate-200"
                  disabled={isLoading}
                />
                <button
                  type="button"
                  onClick={() => setShowCpw(!showCpw)}
                  className="text-slate-300 hover:text-slate-950 transition-colors ml-2"
                >
                  <EyeIcon open={showCpw} />
                </button>
              </div>
              {errors.confirmPassword && <p className="text-[10px] font-bold text-red-500 ml-2">{errors.confirmPassword}</p>}
            </div>

            {(serverError) && (
              <p className="text-xs font-black text-red-500 ml-1">! {serverError}</p>
            )}

            <div className="mt-2">
              <button
                type="submit"
                disabled={isLoading}
                className="w-full h-14 bg-slate-950 hover:bg-slate-800 hover:scale-[1.01] text-white font-black text-lg transition-all active:scale-[0.98] disabled:opacity-50 rounded-2xl"
              >
                {isLoading ? '처리 중...' : '회원가입'}
              </button>
            </div>
          </form>

          <div className="mt-10 pt-6 border-t border-slate-300 flex flex-col items-center gap-4">
            <span className="text-slate-400 text-xs font-medium">이미 계정이 있으신가요?</span>
            <Link
              to="/login"
              className="text-slate-950 text-xs font-black border-b border-slate-950 pb-0.5 hover:text-brand-main hover:border-brand-main transition-all"
            >
              로그인하기
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

export default RegisterPage;
