package com.busnow.repository;

import com.busnow.entity.Stops;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Stops 테이블 JPA Repository.
 *
 * - CSV 적재는 JdbcTemplate.batchUpdate를 사용하므로 이 인터페이스에는 save 계열 미사용.
 * - 실제 사용: 데이터 존재 여부 확인(count), 단건 조회(findById).
 * - 검색(LIKE 쿼리)은 MyBatis Mapper에서 처리.
 */
public interface StopsRepository extends JpaRepository<Stops, String> {
    // findById(String stopId) → JpaRepository 기본 제공
    // count() → 적재 전 데이터 존재 여부 확인용
}
