/**
 * BusArrivalCard.jsx
 * 버스 도착 정보를 표시하는 공통 컴포넌트 (심플 버전)
 */
import React from 'react';

// 버스 유형별 색상
const getRouteColor = (type) => {
  if (!type) return 'bg-slate-500';
  if (type.includes('직행') || type.includes('광역') || type.includes('빨강') || type.includes('좌석')) return 'bg-red-500';
  if (type.includes('일반') || type.includes('시내')) return 'bg-green-500';
  if (type.includes('마을')) return 'bg-yellow-500';
  return 'bg-blue-500';
};

export default function BusArrivalCard({ stopArrival, onToggleFavorite, isFavorite, onAddNotification, onRemoveNotification, activeNotifications = new Set() }) {
  const { stopId, stopName, arrivals, hasError, errorMessage } = stopArrival;

  return (
    <div 
      className="glass-card px-10 animate-fade-in border-t-4 border-t-blue-500 bg-white/95 shadow-2xl rounded-[32px] border border-white/50"
      style={{ paddingTop: '22px', paddingBottom: '16px' }}
    >
      {/* 정류소 헤더: 여백 및 별표 크기 조정 */}
      <div className="flex justify-between" style={{ paddingLeft: '10px', paddingRight: '16px', marginBottom: '24px', alignItems: 'center' }}>
        <div className="flex-1">
          <h3 className="text-3xl font-black text-slate-800 tracking-tight leading-tight">{stopName}</h3>
          <div className="flex items-center gap-2" style={{ marginTop: '4px' }}>
            <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></div>
            <p className="text-sm font-bold text-slate-400 uppercase tracking-wider">실시간 도착 정보</p>
          </div>
        </div>
        {onToggleFavorite && (
          <button 
            onClick={() => onToggleFavorite(stopId)} 
            className="p-3 rounded-2xl hover:bg-yellow-50 transition-all text-slate-300 hover:text-yellow-400 hover:scale-110 active:scale-95"
          >
            <svg width="36" height="36" viewBox="0 0 24 24" fill={isFavorite ? '#FACC15' : 'none'} stroke={isFavorite ? '#FACC15' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </button>
        )}
      </div>

      {/* 도착 정보 리스트 */}
      {hasError ? (
        <div className="bg-red-50 text-red-600 rounded-2xl text-sm font-bold border border-red-100" style={{ padding: '16px' }}>
          {errorMessage || '정보를 불러오지 못했습니다.'}
        </div>
      ) : arrivals && arrivals.length > 0 ? (
        <div>
          {arrivals.map((bus, idx) => {
            const isSoon = bus.isImminent;
            const notifKey = `${stopId}-${bus.routeId}`;
            const isNotified = activeNotifications.has(notifKey);

            return (
              <div 
                key={`${bus.routeId}-${idx}`} 
                className={`flex items-center justify-between bg-slate-50/50 hover:bg-white rounded-2xl border ${isNotified ? 'border-blue-400/50 shadow-sm bg-blue-50/30' : 'border-slate-100'} hover:border-blue-300/50 hover:shadow-md transition-all duration-300`}
                style={{ padding: '16px', marginBottom: '10px' }}
              >
                {/* 버스 번호와 타입 */}
                <div className="flex items-center gap-6">
                  <div className={`w-16 h-10 rounded-xl flex items-center justify-center text-white font-black text-lg shadow-sm ${getRouteColor(bus.routeType)}`}>
                    {bus.routeNo}
                  </div>
                  <div>
                    <p className="text-base font-black text-slate-800 leading-tight">{bus.routeType || '일반'}</p>
                    <p className="text-[10px] text-slate-400 font-bold uppercase" style={{ marginTop: '2px' }}>Active</p>
                  </div>
                </div>

                {/* 시간 및 알림 */}
                <div className="flex items-center gap-5">
                  <p className={`text-lg font-black tracking-tight ${isSoon ? 'text-red-500' : 'text-slate-700'}`}>
                    {bus.arrivalMessage}
                  </p>
                  {onAddNotification && (
                    <button 
                      onClick={(e) => { 
                        e.stopPropagation(); 
                        if (isNotified && onRemoveNotification) {
                          onRemoveNotification(stopId, bus);
                        } else if (!isNotified) {
                          onAddNotification(stopId, bus);
                        }
                      }}
                      className={`p-2.5 rounded-xl transition-all ${
                        isNotified 
                          ? 'text-blue-500 bg-blue-50 hover:bg-red-50 hover:text-red-500' 
                          : 'text-slate-300 hover:text-blue-500 hover:bg-blue-50'
                      } cursor-pointer`}
                      title={isNotified ? "알림 해제" : "알림 추가"}
                    >
                      <svg width="20" height="20" viewBox="0 0 24 24" fill={isNotified ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2.5"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="p-10 text-center text-slate-400 bg-slate-50/50 rounded-2xl text-sm border border-dashed border-slate-200 font-bold">
          현재 도착 예정인 버스가 없습니다.
        </div>
      )}
    </div>
  );
}
