/**
 * SearchPage.jsx
 * 정류소 검색 및 단일 정류소 실시간 조회 (2단 레이아웃 버전)
 */
import { useState, useEffect } from 'react';
import api from '../api/axiosInstance';
import { API } from '../api/apiEndpoints';
import BusArrivalCard from '../components/common/BusArrivalCard';
import { useToast } from '../hooks/useToast';

export default function SearchPage() {
  const [keyword, setKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  
  const [selectedStop, setSelectedStop] = useState(null);
  const [arrivalData, setArrivalData] = useState(null);
  const [isLoadingArrival, setIsLoadingArrival] = useState(false);

  const [favoriteStopIds, setFavoriteStopIds] = useState(new Set());
  const [activeNotifications, setActiveNotifications] = useState(new Set());
  const { showToast } = useToast();
  const [hoveredId, setHoveredId] = useState(null);

  // 초기 즐겨찾기 및 알림 로드
  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const favRes = await api.get(API.FAVORITES.LIST);
        const favIds = new Set(favRes.data.map(f => f.stopId));
        setFavoriteStopIds(favIds);

        const notifRes = await api.get(API.NOTIFICATIONS.LIST);
        const notifSet = new Set(notifRes.data.map(n => `${n.stopId}-${n.routeId}`));
        setActiveNotifications(notifSet);
      } catch (err) {
        console.error('초기 데이터 로드 실패:', err);
      }
    };
    fetchInitialData();
  }, []);

  // 검색 실행
  const handleSearch = async (e) => {
    if (e) e.preventDefault();
    if (!keyword.trim()) return;

    setIsSearching(true);
    setHasSearched(true);
    setArrivalData(null);
    setSelectedStop(null);
    
    try {
      const res = await api.get(`${API.STOPS.SEARCH}?keyword=${encodeURIComponent(keyword)}`);
      setSearchResults(res.data || []);
      if (res.data.length === 0) showToast('검색 결과가 없습니다.', 'info');
    } catch (err) {
      showToast('검색에 실패했습니다.', 'error');
    } finally {
      setIsSearching(false);
    }
  };

  // 정류소 선택
  const handleSelectStop = async (stop) => {
    setSelectedStop(stop);
    setIsLoadingArrival(true);
    try {
      const res = await api.get(API.BUS.ARRIVAL(stop.stopId));
      setArrivalData(res.data);
    } catch (err) {
      showToast('도착 정보를 불러오는데 실패했습니다.', 'error');
      setArrivalData(null);
    } finally {
      setIsLoadingArrival(false);
    }
  };

  // 즐겨찾기 토글
  const toggleFavorite = async (stop, e) => {
    if (e) e.stopPropagation();
    const stopId = typeof stop === 'string' ? stop : stop.stopId;
    const stopName = typeof stop === 'object' ? stop.stopName : arrivalData?.stopName;

    try {
      if (favoriteStopIds.has(stopId)) {
        await api.delete(API.FAVORITES.DELETE_STOP(stopId));
        setFavoriteStopIds(prev => {
          const next = new Set(prev);
          next.delete(stopId);
          return next;
        });
        showToast('즐겨찾기 해제', 'info');
      } else {
        // 백엔드에 stopId와 stopName을 함께 전달하여 자동 생성 지원
        await api.post(API.FAVORITES.ADD, { stopId, stopName });
        setFavoriteStopIds(prev => {
          const next = new Set(prev);
          next.add(stopId);
          return next;
        });
        showToast('즐겨찾기 추가', 'success');
      }
    } catch (err) {
      showToast('즐겨찾기 변경 실패', 'error');
    }
  };

  const handleAddNotification = async (stopId, bus) => {
    try {
      await api.post(API.NOTIFICATIONS.ADD, {
        stopId, 
        stopName: selectedStop.stopName,
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
    <div className="app-container flex flex-col items-center min-h-screen">
      <h1 className="page-title animate-fade-in text-center mb-10">버스 정류소 검색</h1>

      {/* 검색 바 영역 */}
      <div className="w-full flex justify-center z-30">
        <form onSubmit={handleSearch} className="glass-card p-2 rounded-full flex items-center bg-white shadow-xl w-full max-w-4xl border border-white/50">
          <div className="relative flex-1 flex items-center">
            <span className="absolute left-6 text-slate-400">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
              </svg>
            </span>
            <input
              type="text"
              className="premium-input !shadow-none !bg-transparent border-none !py-4 !pl-16 !text-lg w-full"
              placeholder="정류소 이름을 입력하세요"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <button type="submit" className="btn-primary ml-2 px-10 py-4 text-lg">검색</button>
        </form>
      </div>

      {/* 물리적 Spacer: 검색바와 아래 결과 사이 간격 확보 */}
      <div className="h-10 w-full invisible"></div>

      {/* 메인 2단 레이아웃 */}
      <div className="w-full max-w-7xl animate-fade-in">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">
          
          {/* 왼쪽: 검색 결과 (선택 시 너비 조절) */}
          <div className={`${selectedStop ? 'lg:col-span-5' : 'lg:col-span-8 lg:col-start-3'} space-y-6 transition-all duration-500`}>
            {!isSearching && searchResults.length > 0 && (
              <div className="space-y-4">
                <div className="flex items-center gap-3 mb-6 ml-1">
                  <span className="text-[11px] font-black uppercase tracking-widest text-slate-800">Results</span>
                  <p className="text-sm font-bold text-slate-400">{searchResults.length}개의 정류소를 찾았습니다</p>
                </div>
                
                {searchResults.map((stop) => (
                  <div 
                    key={stop.stopId}
                    onMouseEnter={() => setHoveredId(stop.stopId)}
                    onMouseLeave={() => setHoveredId(null)}
                    onClick={() => handleSelectStop(stop)}
                    className="glass-card p-6 flex justify-between items-center cursor-pointer transition-all duration-300 bg-white/70 hover:bg-white"
                    style={{ 
                      border: selectedStop?.stopId === stop.stopId ? '2px solid #2563eb' : '1px solid rgba(255,255,255,0.4)',
                      boxShadow: hoveredId === stop.stopId ? '0 20px 40px -10px rgba(0,0,0,0.15)' : '0 10px 20px -5px rgba(0,0,0,0.05)'
                    }}
                  >
                    <div className="flex items-center gap-6 flex-1">
                      <div 
                        className="w-16 h-16 rounded-2xl flex items-center justify-center transition-all duration-300 shadow-inner"
                        style={{ 
                          backgroundColor: hoveredId === stop.stopId ? '#2563eb' : '#f1f5f9',
                          color: hoveredId === stop.stopId ? '#facc15' : '#64748b'
                        }}
                      >
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                          <path d="M3 21h18"/><path d="M5 21V7a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v14"/><path d="M9 21v-4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v4"/><path d="M7 9h.01"/><path d="M7 13h.01"/><path d="M13 9h.01"/><path d="M13 13h.01"/><path d="M17 9h.01"/><path d="M17 13h.01"/>
                        </svg>
                      </div>
                      <h3 className="font-black text-slate-800 text-lg group-hover:text-brand-main transition-colors">{stop.stopName}</h3>
                    </div>
                    
                    <div className="flex items-center" style={{ marginRight: '12px' }}>
                      <button 
                        onClick={(e) => toggleFavorite(stop, e)}
                        className="p-3 rounded-2xl transition-all text-slate-300 hover:text-yellow-400 hover:bg-yellow-50 hover:scale-110 active:scale-95"
                      >
                        <svg width="36" height="36" viewBox="0 0 24 24" fill={favoriteStopIds.has(stop.stopId) ? '#FACC15' : 'none'} stroke={favoriteStopIds.has(stop.stopId) ? '#FACC15' : 'currentColor'} strokeWidth="2.5">
                          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                        </svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
            {!isSearching && hasSearched && searchResults.length === 0 && (
              <div className="glass-card p-12 text-center bg-white/50 border-dashed border-2 border-slate-200 rounded-[32px] animate-fade-in">
                <h3 className="text-xl font-black text-slate-800 mb-2">검색 결과가 없습니다</h3>
                <p className="text-slate-400 font-semibold text-sm">정류소 이름을 확인하고 다시 검색해 주세요.</p>
              </div>
            )}
          </div>

          {selectedStop && arrivalData && (
            <div className="lg:col-span-7 animate-slide-right">
              <div className="flex justify-between items-center mb-6 px-2">
                <h2 className="text-xl font-black text-slate-800">실시간 도착 정보</h2>
                <button 
                  onClick={() => setSelectedStop(null)} 
                  className="w-10 h-10 flex items-center justify-center bg-slate-100 hover:bg-red-50 hover:text-red-500 rounded-full transition-all text-slate-400 font-black text-xl"
                  title="닫기"
                >
                  ✕
                </button>
              </div>
              <BusArrivalCard 
                stopArrival={{ ...arrivalData, stopName: selectedStop.stopName }} 
                onToggleFavorite={() => toggleFavorite({ ...arrivalData, stopName: selectedStop.stopName })}
                isFavorite={favoriteStopIds.has(arrivalData.stopId)}
                onAddNotification={handleAddNotification}
                onRemoveNotification={handleRemoveNotification}
                activeNotifications={activeNotifications}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
