package com.busnow.repository;

import com.busnow.entity.BusRoutes;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Bus_Routes 테이블 JPA Repository.
 *
 * - CSV 적재는 JdbcTemplate.batchUpdate를 사용하므로 save 계열 미사용.
 * - 실제 사용: 단건 조회(findById), 존재 여부 확인(count).
 */
public interface BusRoutesRepository extends JpaRepository<BusRoutes, String> {
    // findById(String routeId) → JpaRepository 기본 제공
}
