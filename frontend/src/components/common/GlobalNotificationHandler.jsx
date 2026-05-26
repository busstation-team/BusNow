import { useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../hooks/useToast';
import api from '../../api/axiosInstance';
import { API } from '../../api/apiEndpoints';

/**
 * GlobalNotificationHandler
 * 사용자가 로그인한 상태라면 어느 페이지에 있든 15초마다 도착 정보를 확인하여 브라우저 알림을 띄웁니다.
 */
export default function GlobalNotificationHandler() {
  const { user, isLoading } = useAuth();
  const { showToast } = useToast();
  const notifiedIds = useRef(new Set());

  useEffect(() => {
    // ⚠️ 핵심: 토큰 복구 중이거나(isLoading) 유저가 없으면 절대 실행하지 않음!
    if (!user || isLoading) {
      notifiedIds.current.clear();
      return;
    }

    // 초기 브라우저 알림 권한 요청
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }

    const checkArrivals = async () => {
      try {
        // 1. 활성화된 알림 설정 목록 조회
        const notifRes = await api.get(API.NOTIFICATIONS.LIST);
        const activeNotifs = (notifRes.data || []).filter(n => n.isActive);
        if (activeNotifs.length === 0) return;

        // 2. 실시간 정류소 정보 조회 (즐겨찾기 기준)
        const arrivalRes = await api.get(API.FAVORITES.ARRIVAL);
        const stops = arrivalRes.data || [];

        // 3. 알림 발송 조건 매칭
        console.log(`[GlobalNotif] 체크 시작 - 활성알림:${activeNotifs.length}개, 정류소:${stops.length}개`);
        
        stops.forEach(stop => {
          if (!stop.arrivals) return;

          stop.arrivals.forEach(bus => {
            const target = activeNotifs.find(n => 
              String(n.stopId) === String(stop.stopId) && 
              String(n.routeId) === String(bus.routeId)
            );

            if (target) {
              // 프론트엔드용 DTO(ArrivalInfoResponse)의 초 단위 필드명은 traTime입니다.
              const arrivalSec = bus.traTime; 
              const alertSec = target.alertTimeMin * 60;
              const remainMin = Math.ceil(arrivalSec / 60);
              
              // ✅ 1회 알림 + 15분 쿨타임 전략: 분(min)을 키에서 제거하여 한 번만 울리게 함
              const key = `noti-${target.notificationId}`;

              console.log(`[GlobalNotif] 매칭 후보 발견: ${target.routeName}번 (${arrivalSec}초 남음 / 설정:${alertSec}초)`);

              // 설정한 시간 이하로 남았고 아직 이 시점에 알림을 보낸 적이 없는 경우 (0초 포함)
              if (arrivalSec >= 0 && arrivalSec <= alertSec && !notifiedIds.current.has(key)) {
                const message = `[${target.routeName}] 버스가 ${stop.stopName} 정류소에 ${remainMin}분 후 도착합니다!`;
                console.log(`🚀 [GlobalNotif] 알림 발송 조건 충족! -> ${target.routeName}번`);
                
                // 1. 브라우저 시스템 알림
                if (Notification.permission === 'granted') {
                  try {
                    new Notification('BusNow 도착 알림 🚌', {
                      body: message,
                      icon: '/favicon.ico', // 파비콘 아이콘 추가
                      requireInteraction: true // 사용자가 닫기 전까지 화면에 유지
                    });
                    console.log('✅ 브라우저 팝업 호출 성공');
                  } catch (e) {
                    console.error('❌ 브라우저 팝업 호출 실패:', e);
                  }
                }

                // 2. 앱 내 토스트 알림 (브라우저 알림이 안 보일 경우를 대비)
                showToast(message, 'success');
                console.log('✅ 앱 내 토스트 알림 표시 성공');

                notifiedIds.current.add(key);
                
                // ✅ 유동적 쿨타임: 버스가 도착하기까지 남은 시간(arrivalSec) + 여유 시간 5분(300초)
                // 설정한 시간이 20분이라면 쿨타임은 25분이 되어, 버스가 완전히 떠날 때까지 절대 중복 알림이 울리지 않습니다.
                const cooldownMs = (arrivalSec * 1000) + (5 * 60 * 1000);
                
                setTimeout(() => {
                  notifiedIds.current.delete(key);
                }, cooldownMs);
              }
            }
          });
        });
      } catch (err) {
        console.error('[GlobalNotification] Error checking arrivals:', err);
      }
    };

    // 25초마다 주기적으로 체크 (서버 부하 감소)
    const interval = setInterval(checkArrivals, 25000);
    
    // 첫 실행
    checkArrivals();

    return () => clearInterval(interval);
  }, [user]);

  return null;
}
