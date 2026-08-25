package com.barrierfree.bf.taxi.repository;

import com.barrierfree.bf.taxi.entity.TaxiReservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxiReservationRepository extends JpaRepository<TaxiReservation, Long> {

  /** 특정 사용자의 예약 이력 조회 */
  List<TaxiReservation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
