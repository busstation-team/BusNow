/**
 * FavoritesPage.jsx
 * 즐겨찾기 관리 페이지 (SearchPage와 유사한 프리미엄 레이아웃 적용)
 */
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosInstance';
import { API } from '../api/apiEndpoints';
import { useToast } from '../hooks/useToast';
import BusArrivalCard from '../components/common/BusArrivalCard';

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState([]);
  const [activeNotifications, setActiveNotifications] = useState(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [selectedFav, setSelectedFav] = useState(null);
  const [arrivalData, setArrivalData] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  
  // 편집용 상태
  const [editingId, setEditingId] = useState(null);
  const [editAlias, setEditAlias] = useState('');
  
  const { showToast } = useToast();
  const navigate = useNavigate();

  // 즐겨찾기 목록 불러오기
  const fetchFavorites = async () => {
    setIsLoading(true);
    try {
      const res = await api.get(API.FAVORITES.LIST);
      setFavorites(res.data || []);
      if (res.data && res.data.length > 0 && !selectedFav) {
        handleSelectFavorite(res.data[0]);
      }

      // Fetch active notifications
      const notifRes = await api.get(API.NOTIFICATIONS.LIST);
      const notifSet = new Set(notifRes.data.map(n => `${n.stopId}-${n.routeId}`));
      setActiveNotifications(notifSet);
    } catch (err) {
      showToast('초기 데이터를 불러오지 못했습니다.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // 실시간 도착 정보 불러오기
  const fetchArrivalInfo = async (stopId, cityCode) => {
    setIsRefreshing(true);
    try {
      const res = await api.get(API.BUS.ARRIVAL(stopId, cityCode));
      setArrivalData(res.data);
    } catch (err) {
      showToast('도착 정보를 불러오지 못했습니다.', 'error');
    } finally {
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchFavorites();
  }, []);

  // 정류소 선택 핸들러
  const handleSelectFavorite = (fav) => {
    setSelectedFav(fav);
    fetchArrivalInfo(fav.stopId, fav.cityCode);
  };

  // 삭제 핸들러
  const handleDelete = async (id, e) => {
    e.stopPropagation();
    if (!window.confirm('이 즐겨찾기를 삭제하시겠습니까?')) return;
    try {
      await api.delete(API.FAVORITES.DELETE(id));
      setFavorites(prev => prev.filter(f => f.favoriteId !== id));
      if (selectedFav?.favoriteId === id) {
        setSelectedFav(null);
        setArrivalData(null);
      }
      showToast('삭제되었습니다.', 'success');
    } catch (err) {
      showToast('삭제에 실패했습니다.', 'error');
    }
  };

  // 별칭 수정 시작
  const startEdit = (fav, e) => {
    e.stopPropagation();
    setEditingId(fav.favoriteId);
    setEditAlias(fav.alias || '');
  };

  // 별칭 저장
  const saveEdit = async (id, e) => {
    e.stopPropagation();
    try {
      const res = await api.patch(API.FAVORITES.ALIAS(id), { alias: editAlias });
      setFavorites(prev => prev.map(f => f.favoriteId === id ? res.data : f));
      if (selectedFav?.favoriteId === id) {
        setSelectedFav(res.data);
      }
      setEditingId(null);
      showToast('별칭이 수정되었습니다.', 'success');
    } catch (err) {
      showToast('별칭 수정에 실패했습니다.', 'error');
    }
  };

  const handleAddNotification = async (stopId, bus) => {
    try {
      await api.post(API.NOTIFICATIONS.ADD, {
        stopId, 
        stopName: selectedFav.stopName,
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

  if (isLoading) {
    return (
      <div className="app-container flex items-center justify-center min-h-[60vh]">
        <div className="w-12 h-12 border-4 border-brand-main border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="app-container animate-fade-in min-h-screen">
      <h1 className="page-title text-center mb-12">즐겨찾기 관리</h1>

      {favorites.length === 0 ? (
        /* 즐겨찾기 없음 - 비어 있는 상태 (MainPage와 동일) */
        <div className="max-w-md mx-auto text-center px-4" style={{ marginTop: '30px' }}>
          <div 
            className="glass-card px-10 bg-white/40 border-2 border-white/50 flex flex-col items-center shadow-2xl rounded-[40px]"
            style={{ paddingTop: '48px', paddingBottom: '48px' }}
          >
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
              자주 타는 버스 정류소를 검색해서<br/>
              나만의 즐겨찾기 목록을 만들어보세요!
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
        /* 즐겨찾기 목록 - 2단 레이아웃 (SearchPage와 유사) */
        <div className="w-full max-w-7xl mx-auto">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">
            
            {/* 왼쪽: 즐겨찾기 목록 */}
            <div className="lg:col-span-5 flex flex-col gap-4">
              <div className="flex items-center gap-3 mb-6 ml-1">
                <span className="text-[11px] font-black uppercase tracking-widest text-slate-800">My Favorites</span>
                <p className="text-sm font-bold text-slate-400">{favorites.length}개의 정류소</p>
              </div>

              {favorites.map((fav) => (
                <div 
                  key={fav.favoriteId}
                  onClick={() => handleSelectFavorite(fav)}
                  className={`glass-card min-h-[80px] py-4 px-8 flex justify-between items-center cursor-pointer transition-all duration-300
                    ${selectedFav?.favoriteId === fav.favoriteId 
                      ? 'bg-white border-brand-main shadow-xl scale-[1.02]' 
                      : 'bg-white/70 hover:bg-white'}`}
                  style={{ 
                    border: selectedFav?.favoriteId === fav.favoriteId ? '2px solid #2563eb' : '1px solid rgba(255,255,255,0.4)',
                    borderRadius: '8px'
                  }}
                >
                  <div className="flex-1 min-w-0 pr-4" style={{ paddingLeft: '24px' }}>
                    {editingId === fav.favoriteId ? (
                      <div className="flex gap-2 items-center" onClick={e => e.stopPropagation()}>
                        <input 
                          type="text" 
                          value={editAlias} 
                          onChange={e => setEditAlias(e.target.value)}
                          className="premium-input !py-2 !px-3 !text-sm flex-1"
                          autoFocus
                        />
                        <button onClick={(e) => saveEdit(fav.favoriteId, e)} className="p-2 bg-green-500 text-white rounded-md hover:bg-green-600 transition-colors shadow-sm">
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.5"><path d="M20 6L9 17l-5-5"/></svg>
                        </button>
                        <button onClick={(e) => { e.stopPropagation(); setEditingId(null); }} className="p-2 bg-slate-200 text-slate-600 rounded-md">
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3"><path d="M18 6L6 18M6 6l12 12"/></svg>
                        </button>
                      </div>
                    ) : (
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-black text-slate-800 text-lg truncate">{fav.alias || fav.stopName}</h3>
                        </div>
                        <p className="text-xs text-slate-400 mt-1 font-bold">{fav.stopName}</p>
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-2">
                    <button 
                      onClick={(e) => startEdit(fav, e)}
                      className="w-10 h-10 flex items-center justify-center bg-blue-50 text-brand-main rounded-lg hover:bg-brand-main hover:text-white transition-all shadow-sm"
                      title="별칭 수정"
                    >
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                    </button>
                    <button 
                      onClick={(e) => handleDelete(fav.favoriteId, e)}
                      className="w-10 h-10 flex items-center justify-center bg-red-50 text-red-500 rounded-lg hover:bg-red-500 hover:text-white transition-all shadow-sm"
                      title="삭제"
                    >
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                      </svg>
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* 오른쪽: 실시간 도착 정보 (SearchPage와 동일) */}
            <div className="lg:col-span-7 animate-slide-right">
              {selectedFav && arrivalData ? (
                <div className="space-y-6">
                  <div className="flex justify-between items-center px-2">
                    <h2 className="text-xl font-black text-slate-800">실시간 도착 정보</h2>
                  </div>
                  <BusArrivalCard 
                    stopArrival={{ ...arrivalData, stopName: selectedFav.alias || selectedFav.stopName }} 
                    onToggleFavorite={(id) => {}} // 관리 페이지이므로 목록에서 처리
                    isFavorite={true}
                    onAddNotification={handleAddNotification}
                    onRemoveNotification={handleRemoveNotification}
                    activeNotifications={activeNotifications}
                  />
                </div>
              ) : (
                <div className="h-[400px] glass-card flex flex-col items-center justify-center text-slate-300">
                  <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" className="mb-4 opacity-20">
                    <path d="M12 2v20M2 12h20"/>
                  </svg>
                  <p className="font-bold">정류소를 선택하여 도착 정보를 확인하세요</p>
                </div>
              )}
            </div>

          </div>
        </div>
      )}
    </div>
  );
}
