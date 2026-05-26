package com.busnow.dto.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 즐겨찾기 등록 요청 DTO.
 *
 * @param stopId   등록할 정류소 ID (Stops.Stop_id, 필수)
 * @param stopName 정류소 명칭 (Stops.Stop_name, 필수 - DB에 없을 경우 자동 생성을 위함)
 * @param alias    사용자 지정 별칭 (Favorite_Stops.Alias, 선택 · 최대 50자)
 */
public record FavoriteStopRequest(
        @NotBlank(message = "정류소 ID를 입력해주세요.")
        String stopId,

        @NotBlank(message = "정류소 명칭을 입력해주세요.")
        String stopName,

        @Size(max = 50, message = "별칭은 최대 50자까지 입력할 수 있습니다.")
        String alias
) {}
