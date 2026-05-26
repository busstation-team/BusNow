/**
 * Toast.jsx
 * 전역 토스트 알림 컴포넌트
 *
 * 사용법:
 *   import { useToast } from '../../hooks/useToast';
 *   const { showToast } = useToast();
 *   showToast('저장되었습니다.', 'success');
 */
import { useEffect } from 'react';

const ICONS = {
  success: '✓',
  error:   '✕',
  info:    'ℹ',
};

export function ToastItem({ id, message, type = 'info', onDismiss }) {
  useEffect(() => {
    const timer = setTimeout(() => onDismiss(id), 3000);
    return () => clearTimeout(timer);
  }, [id, onDismiss]);

  return (
    <div
      className={`toast toast-${type} animate-slide-up`}
      onClick={() => onDismiss(id)}
      style={{ cursor: 'pointer' }}
    >
      <span className="mr-2 font-bold">{ICONS[type]}</span>
      {message}
    </div>
  );
}

export function ToastContainer({ toasts, onDismiss }) {
  if (toasts.length === 0) return null;
  return (
    <div className="toast-container">
      {toasts.map((t) => (
        <ToastItem key={t.id} {...t} onDismiss={onDismiss} />
      ))}
    </div>
  );
}
