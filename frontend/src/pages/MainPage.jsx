/**
 * MainPage.jsx
 * 메인 대시보드 - 즐겨찾기 정류소의 실시간 도착 정보를 폴링
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosInstance';
import { API } from '../api/apiEndpoints';
import BusArrivalCard from '../components/common/BusArrivalCard';
import { useToast } from '../hooks/useToast';

export default function MainPage() {
  const [activeNotifications, setActiveNotifications] = useState(new Set());
  const [notifications, setNotifications] = useState([]); // 알림 설정 원본 데이터
  const [isLoading, setIsLoading] = useState(true);
  
  // 이미 알림을 보낸 버스 ID 추적 (중복 팝업 방지)
  const notifiedIds = useRef(new Set());
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [isRefreshing, setIsRefreshing] = useState(false);
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [favorites, setFavorites] = useState([]);

  // 브라우저 알림 권한 요청
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  const fetchArrivals = useCallback(async (isBackground = false) => {
    if (!isBackground) setIsLoading(true);
    else setIsRefreshing(true);
    
    try {
      const res = await api.get(API.FAVORITES.ARRIVAL);
      const arrivalsData = res.data || [];
      console.log('[MainPage] 즐겨찾기 데이터 수신:', arrivalsData.length, '개');
      setFavorites(arrivalsData);
      setLastUpdated(new Date());

      // 알림 설정 목록 가져오기
      const notifRes = await api.get(API.NOTIFICATIONS.LIST);
      const notifList = notifRes.data || [];
      setNotifications(notifList);
      
      const notifSet = new Set(notifList.map(n => `${n.stopId}-${n.routeId}`));
      setActiveNotifications(notifSet);

    } catch (err) {
      console.error('Failed to fetch arrivals', err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);


  // 초기 로딩 및 폴링 설정
  useEffect(() => {
    fetchArrivals(false);
    
    const interval = setInterval(() => {
      fetchArrivals(true);
    }, 10000); // 10초마다 갱신
    
    return () => clearInterval(interval);
  }, [fetchArrivals]);

  if (isLoading) {
    return (
      <div className="app-container flex flex-col items-center justify-center min-h-[60vh]">
        <div className="relative">
          <div className="w-16 h-16 border-4 border-brand-main/20 border-t-brand-main rounded-full animate-spin"></div>
          <div className="absolute inset-0 flex items-center justify-center">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-brand-main">
              <rect x="1" y="3" width="15" height="13" rx="2"/><path d="M16 8h4l3 3v3h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>
            </svg>
          </div>
        </div>
        <p className="mt-6 text-slate-500 font-bold animate-pulse uppercase tracking-widest text-xs">Bus Arrival Data Loading...</p>
      </div>
    );
  }

  const handleAddNotification = async (stopId, bus, stopName) => {
    try {
      await api.post(API.NOTIFICATIONS.ADD, {
        stopId, 
        stopName: stopName,
        routeId: bus.routeId,
        routeName: bus.routeNo,
        routeType: bus.routeType,
        alertTime: 180, 
        isActive: true
      });
      setActiveNotifications(prev => new Set(prev).add(`${stopId}-${bus.routeId}`));
      showToast('도착 알림이 등록되었습니다. 이 노선의 모든 버스에 대해 알림이 제공됩니다.', 'success');
    } catch (err) {
      const msg = err.response?.data?.message || '알림 추가 실패';
      showToast(msg, 'error');
    }
  };

  const handleRemoveNotification = async (stopId, bus) => {
    try {
      await api.delete(API.NOTIFICATIONS.DELETE_BY_ROUTE(stopId, bus.routeId));
      setActiveNotifications(prev => {
        const next = new Set(prev);
        next.delete(`${stopId}-${bus.routeId}`);
        return next;
      });
      showToast('알림이 해제되었습니다.', 'info');
    } catch (err) {
      showToast('알림 해제 실패', 'error');
    }
  };

  return (
    <div className="app-container animate-fade-in">
      {/* 헤더 섹션: 더 넓고 심플하게 정리 */}
      <div className="mt-4 mb-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="page-title !text-left !mb-0">내 정류소 도착 정보</h1>
          </div>
          <p 
            className="text-slate-400 font-medium flex items-center gap-2"
            style={{ marginTop: '16px' }}
          >
            <span className="w-2 h-2 bg-green-500 rounded-full animate-ping"></span>
            10초마다 실시간으로 자동 갱신됩니다.
          </p>
        </div>
      </div>

      {/* 도착 정보 그리드 */}
      {favorites.length === 0 ? (
        <div className="max-w-md mx-auto text-center animate-fade-in px-4" style={{ marginTop: '30px' }}>
          <div 
            className="glass-card px-10 bg-white/40 border-2 border-white/50 flex flex-col items-center shadow-2xl rounded-[40px]"
            style={{ paddingTop: '48px', paddingBottom: '48px' }}
          >
            {/* 별 박스 여백 적당히 */}
            <div 
              className="w-24 h-24 bg-yellow-50 rounded-[32px] flex items-center justify-center shadow-inner border border-yellow-100/50"
              style={{ marginBottom: '24px' }}
            >
              <span className="text-5xl">⭐</span>
            </div>
            
            <h2 className="text-2xl font-black text-slate-800 tracking-tight" style={{ marginBottom: '12px' }}>
              즐겨찾기가 비어있습니다
            </h2>
            
            <p className="text-slate-500 font-semibold leading-relaxed text-sm" style={{ marginBottom: '32px' }}>
              자주 타는 버스 정류소를 즐겨찾기에 추가하고<br/>
              실시간 도착 정보를 대시보드에서 바로 확인하세요!
            </p>
            
            <button 
              onClick={() => navigate('/search')} 
              className="btn-primary w-full py-5 text-xl !shadow-none hover:scale-105 active:scale-95 transition-all"
              style={{ borderRadius: '12px' }}
            >
              정류소 검색하러 가기
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {favorites.map(stop => (
            <BusArrivalCard 
              key={stop.stopId} 
              stopArrival={stop} 
              onAddNotification={(stopId, bus) => handleAddNotification(stopId, bus, stop.stopName)}
              onRemoveNotification={handleRemoveNotification}
              activeNotifications={activeNotifications}
            />
          ))}
        </div>
      )}
    </div>
  );
}
