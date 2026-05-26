package com.busnow.dto.favorite;

import com.busnow.entity.FavoriteStops;

import java.time.LocalDateTime;

/**
 * 즐겨찾기 응답 DTO (프론트엔드 전달용).
 *
 *  Entity → DTO 변환:
 *    FavoriteStops 엔티티에서 필요한 데이터만 선별.
 *    LAZY 로딩된 stop의 필드(stopId, stopName)도 포함.
 *    → 서비스 계층에서 트랜잭션 내에서 변환해야 LazyInitializationException 방지.
 *
 * @param favoriteId 즐겨찾기 ID (Favorite_Stops.Favorite_id)
 * @param stopId     정류소 ID (Stops.Stop_id)
 * @param stopName   정류소 명칭 (Stops.Stop_name)
 * @param alias      사용자 지정 별칭 (null이면 stopName으로 표시)
 * @param createdAt  즐겨찾기 등록 시각
 */
public record FavoriteStopResponse(
        Integer favoriteId,
        String stopId,
        String stopName,
        String alias,
        LocalDateTime createdAt
) {
    /**
     * FavoriteStops 엔티티를 응답 DTO로 변환하는 팩토리 메서드.
     *  반드시 트랜잭션 내에서 호출 (f.getStop()이 LAZY 로딩이므로).
     */
    public static FavoriteStopResponse from(FavoriteStops f) {
        return new FavoriteStopResponse(
                f.getFavoriteId(),
                f.getStop().getStopId(),
                f.getStop().getStopName(),
                f.getAlias(),
                f.getCreatedAt()
        );
    }

    /**
     * 화면 표시 이름: 별칭이 있으면 별칭, 없으면 정류소 원래 이름.
     */
    public String displayName() {
        return (alias != null && !alias.isBlank()) ? alias : stopName;
    }
}
