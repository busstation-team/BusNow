import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosInstance';
import { API } from '../api/apiEndpoints';
import { useToast } from '../hooks/useToast';

// 버스 유형별 색상 (BusArrivalCard와 동일)
const getRouteColor = (type) => {
  if (!type) return 'bg-slate-700';
  if (type.includes('직행') || type.includes('광역') || type.includes('빨강') || type.includes('좌석')) return 'bg-red-500';
  if (type.includes('일반') || type.includes('시내')) return 'bg-green-500';
  if (type.includes('마을')) return 'bg-yellow-500';
  return 'bg-blue-500';
};

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editMin, setEditMin] = useState(3);

  const { showToast } = useToast();
  const navigate = useNavigate();

  const fetchNotifications = async () => {
    setIsLoading(true);
    try {
      const res = await api.get(API.NOTIFICATIONS.LIST);
      setNotifications(res.data || []);
    } catch (err) {
      showToast('알림 목록을 불러오지 못했습니다.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const toggleActive = async (id) => {
    try {
      const res = await api.patch(API.NOTIFICATIONS.TOGGLE(id));
      setNotifications(prev => prev.map(n => n.notificationId === id ? { ...n, isActive: res.data.isActive } : n));
    } catch (err) {
      showToast('상태 변경에 실패했습니다.', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('이 알림을 삭제하시겠습니까?')) return;
    try {
      await api.delete(API.NOTIFICATIONS.DELETE(id));
      setNotifications(prev => prev.filter(n => n.notificationId !== id));
      showToast('삭제되었습니다.', 'success');
    } catch (err) {
      showToast('삭제에 실패했습니다.', 'error');
    }
  };

  const startEdit = (notif) => {
    setEditingId(notif.notificationId);
    setEditMin(notif.alertTimeMin || 3);
  };

  const saveEdit = async (id) => {
    if (editMin < 1) {
      showToast('알림 시간은 최소 1분 이상이어야 합니다.', 'error');
      return;
    }
    try {
      const alertTimeSec = editMin * 60;
      const res = await api.patch(API.NOTIFICATIONS.UPDATE_TIME(id), { alertTime: alertTimeSec });
      setNotifications(prev => prev.map(n => n.notificationId === id ? res.data : n));
      setEditingId(null);
      showToast('알림 시간이 변경되었습니다.', 'success');
    } catch (err) {
      showToast('알림 시간 변경에 실패했습니다.', 'error');
    }
  };

  if (isLoading) {
    return (
      <div className="app-container flex items-center justify-center min-h-[60vh]">
        <div className="w-12 h-12 border-4 border-brand-main border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="app-container animate-fade-in min-h-screen">
      <h1 className="page-title text-center mb-12">알림 설정 센터</h1>

      {notifications.length === 0 ? (
        <div className="max-w-md mx-auto text-center px-4" style={{ marginTop: '30px' }}>
          <div
            className="glass-card px-10 bg-white/40 border-2 border-white/50 flex flex-col items-center shadow-2xl rounded-[40px]"
            style={{ paddingTop: '48px', paddingBottom: '48px' }}
          >
            <div
              className="w-24 h-24 bg-blue-50 rounded-[32px] flex items-center justify-center shadow-inner border border-blue-100/50"
              style={{ marginBottom: '24px' }}
            >
              <span className="text-5xl">🔔</span>
            </div>
            <h2 className="text-2xl font-black text-slate-800 tracking-tight" style={{ marginBottom: '12px' }}>
              활성화된 알림이 없습니다
            </h2>
            <p className="text-slate-500 font-semibold leading-relaxed text-sm" style={{ marginBottom: '32px' }}>
              정류소 검색 후 도착 예정 노선 옆의<br />
              알림 아이콘을 눌러 실시간 알림을 받아보세요!
            </p>
            <button
              onClick={() => navigate('/search')}
              className="btn-primary w-full py-5 text-xl !shadow-none hover:scale-105 active:scale-95 transition-all"
              style={{ borderRadius: '12px' }}
            >
              알림 설정하러 가기
            </button>
          </div>
        </div>
      ) : (
        <div className="w-full max-w-5xl mx-auto">
          <div className="flex flex-col gap-6">
            <div className="flex items-center gap-3 mb-2 ml-1">
              <span className="text-[11px] font-black uppercase tracking-widest text-slate-800">Active Notifications</span>
              <p className="text-sm font-bold text-slate-400">{notifications.length}개의 스마트 알림</p>
            </div>

            {notifications.map((notif) => (
              <div
                key={notif.notificationId}
                className={`glass-card p-6 flex flex-col sm:flex-row justify-between items-center transition-all duration-300
                  ${notif.isActive ? 'bg-white border-brand-main/10' : 'bg-white/40 opacity-70 grayscale-[0.3]'}`}
                style={{ borderRadius: '16px' }}
              >
                <div className="flex items-center gap-6 flex-1 w-full sm:w-auto mb-4 sm:mb-0">
                  {/* 버스 번호 뱃지 (유형별 색상 적용) */}
                  <div className={`w-20 h-20 rounded-3xl flex flex-col items-center justify-center shadow-lg border border-white/20 ${getRouteColor(notif.routeType)}`}>
                    <span className="text-xl font-black text-white">{notif.routeName}</span>
                    <span className="text-[9px] font-black text-white/80 uppercase tracking-widest mt-1">
                      {(!notif.routeType || notif.routeType === '??') ? '버스' : notif.routeType.substring(0, 2)}
                    </span>
                  </div>

                  <div className="flex-1 min-w-0">
                    <h3 className="font-black text-slate-800 text-xl truncate">{notif.stopName}</h3>


                    <div className="flex items-center gap-2 mt-3 min-h-[32px]">
                      {editingId === notif.notificationId ? (
                        <div className="flex items-center gap-3 whitespace-nowrap">
                          <div className="flex items-center gap-1.5">
                            <input
                              type="number"
                              min="1"
                              value={editMin}
                              onChange={e => setEditMin(parseInt(e.target.value) || 1)}
                              onKeyDown={(e) => e.key === 'Enter' && saveEdit(notif.notificationId)}
                              className="w-16 premium-input !py-1.5 !px-2 !text-sm text-center border-brand-main/30"
                              autoFocus
                            />
                            <span className="text-xs font-black text-slate-600">분 전</span>
                          </div>
                          <div className="flex gap-1.5">
                            <button
                              onClick={() => saveEdit(notif.notificationId)}
                              className="p-1.5 bg-green-500 text-white rounded-md hover:bg-green-600 shadow-sm"
                              title="저장"
                            >
                              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="4"><path d="M20 6L9 17l-5-5" /></svg>
                            </button>
                            <button 
                              onClick={() => setEditingId(null)} 
                              className="p-1.5 bg-red-500 text-white rounded-md hover:bg-red-600 shadow-sm"
                              title="취소"
                            >
                              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="4"><path d="M18 6L6 18M6 6l12 12"/></svg>
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-black bg-brand-main/5 text-brand-main px-3 py-1.5 rounded-full flex items-center gap-1.5">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
                            {notif.alertTimeMin}분 전 알림
                          </span>
                          <button onClick={() => startEdit(notif)} className="text-[11px] font-bold text-orange-600 hover:text-orange-700 underline uppercase tracking-tighter">Edit Time</button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-6 w-full sm:w-auto justify-end border-t sm:border-t-0 pt-4 sm:pt-0 border-slate-100">
                  <button onClick={() => toggleActive(notif.notificationId)} className="group flex items-center gap-3">
                    <span className={`text-[12px] font-black uppercase tracking-widest transition-colors ${notif.isActive ? 'text-green-600' : 'text-slate-400'}`}>
                      {notif.isActive ? 'Active' : 'Disabled'}
                    </span>
                    <div className={`w-12 h-6 rounded-full relative transition-all duration-300 ${notif.isActive ? 'bg-green-500 shadow-md' : 'bg-slate-300'}`}>
                      <div className={`absolute top-1 bg-white w-4 h-4 rounded-full shadow-sm transition-all duration-300 ${notif.isActive ? 'left-7' : 'left-1'}`} />
                    </div>
                  </button>

                  <button
                    onClick={() => handleDelete(notif.notificationId)}
                    className="w-10 h-10 flex items-center justify-center bg-red-50 text-red-500 rounded-lg hover:bg-red-500 hover:text-white transition-all shadow-sm"
                    style={{ marginRight: '16px' }}
                    title="알림 삭제"
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M3 6h18" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
