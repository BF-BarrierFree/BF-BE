package com.barrierfree.bf.taxi.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.taxi.dto.TaxiCenterResponse;
import com.barrierfree.bf.taxi.dto.TaxiReservationRequest;
import com.barrierfree.bf.taxi.entity.TaxiCenter;
import com.barrierfree.bf.taxi.entity.TaxiReservation;
import com.barrierfree.bf.taxi.repository.TaxiCenterRepository;
import com.barrierfree.bf.taxi.repository.TaxiReservationRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxiReservationService {

  private final TaxiCenterRepository taxiCenterRepository;
  private final TaxiReservationRepository taxiReservationRepository;
  private final UserRepository userRepository;
  private final OpenRouterService openRouterService;

  /** 사용자의 위경도를 기준으로 지정된 반경 내 가장 가까운 센터 N개를 찾아 반환합니다. */
  @Transactional(readOnly = true)
  public List<TaxiCenterResponse> findNearestCenters(
      double userLat, double userLng, double maxDistanceKm, int limit) {
    List<TaxiCenter> allCenters = taxiCenterRepository.findAll();

    return allCenters.stream()
        .map(
            center -> {
              double distance =
                  calculateHaversineDistance(
                      userLat, userLng, center.getLatitude(), center.getLongitude());
              return TaxiCenterResponse.from(center, distance);
            })
        .filter(response -> response.getDistanceKm() <= maxDistanceKm) // 반경 제한 필터링
        .sorted(Comparator.comparingDouble(TaxiCenterResponse::getDistanceKm)) // 거리순 오름차순 정렬
        .limit(limit) // 상위 N개 제한
        .toList();
  }

  /** 요금 규정과 거리, 출발/도착지를 이용해 예상 요금을 산출합니다. (LLM 실시간 호출) */
  @Transactional(readOnly = true)
  public String estimateFare(TaxiReservationRequest.FareEstimate request) {
    TaxiCenter center =
        taxiCenterRepository
            .findById(request.getCenterId())
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));

    return openRouterService.estimateFare(
        request.getDistanceKm(),
        request.getStartAddr(),
        request.getEndAddr(),
        center.getBasicChargeExplanation(),
        center.getExtraChargeExplanation());
  }

  /** 사용자의 최종 예약 시도 기록을 데이터베이스에 저장합니다. */
  @Transactional
  public void saveReservationHistory(
      Long userId, TaxiReservationRequest.CreateReservation request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    TaxiCenter center =
        taxiCenterRepository
            .findById(request.getCenterId())
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));

    TaxiReservation reservation =
        TaxiReservation.builder()
            .user(user)
            .taxiCenter(center)
            .departureAddress(request.getStartAddr())
            .destinationAddress(request.getEndAddr())
            .estimatedFare(request.getEstimatedFare())
            .generatedMessage(request.getGeneratedMessage())
            .userInputMetadata(request.getUserMetadataJson())
            .build();

    taxiReservationRepository.save(reservation);
  }

  /** 두 위경도 좌표 간의 직선 거리(km)를 구하는 Haversine 공식 */
  private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371.0; // 지구 반지름 (km)
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}
