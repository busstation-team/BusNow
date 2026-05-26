/**
 * useToast.js
 * 토스트 알림 상태 관리 커스텀 훅 및 Context
 */
import React, { useState, useCallback, createContext, useContext } from 'react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const showToast = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type }]);
  }, []);

  const dismissToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // JSX 문법을 사용하지 않고 React.createElement를 사용하여 .js 파일에서의 파싱 에러 방지
  return React.createElement(
    ToastContext.Provider,
    { value: { toasts, showToast, dismissToast } },
    children
  );
}

export function useToast() {

  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
