package com.barrierfree.bf.taxi.repository;

import com.barrierfree.bf.taxi.entity.TaxiCenter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxiCenterRepository extends JpaRepository<TaxiCenter, Long> {

  /** 특정 센터 단건 조회 (기존 로직 유지용) */
  Optional<TaxiCenter> findByCenterId(String centerId);

  /** N+1 문제 해결을 위한 In 쿼리 벌크 조회 (Batch용) */
  List<TaxiCenter> findByCenterIdIn(List<String> centerIds);
}
