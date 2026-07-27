package com.barrierfree.bf.route.service;

import com.barrierfree.bf.route.dto.VehicleRouteResponse;
import org.springframework.stereotype.Service;

@Service
public class RoutePricingService {

  public VehicleRouteResponse.FareEstimate estimateTaxi(double distanceMeters, Region region) {
    int estimatedFare =
        estimateFare(
            distanceMeters,
            region.baseDistanceMeter(),
            region.baseFareWon(),
            region.incrementMeter(),
            region.incrementFareWon());
    return new VehicleRouteResponse.FareEstimate(
        region.baseFareWon(),
        estimatedFare,
        region.taxiWaitMinutes(),
        region.providerCategory(),
        region.note());
  }

  public VehicleRouteResponse.FareEstimate estimateAccessibleTaxi(double distanceMeters, Region region) {
    int estimatedFare =
        estimateFare(
            distanceMeters,
            region.baseDistanceMeter(),
            region.accessibleBaseFareWon(),
            region.incrementMeter(),
            region.accessibleIncrementFareWon());
    return new VehicleRouteResponse.FareEstimate(
        region.accessibleBaseFareWon(),
        estimatedFare,
        region.accessibleWaitMinutes(),
        region.providerCategory(),
        region.note());
  }

  public Region classify(double lat, double lng) {
    if (inSeoul(lat, lng)) {
      return new Region("SEOUL", "Seoul", 4800, 4800, 1600, 132, 100, 120, 15, 30, "PUBLIC_AND_PRIVATE", "Seoul distance-based estimate");
    }
    if (inIncheon(lat, lng)) {
      return new Region("INCHEON", "Incheon", 4300, 4800, 1600, 134, 100, 120, 20, 35, "PUBLIC_AND_PRIVATE", "Incheon distance-based estimate");
    }
    if (inBusan(lat, lng)) {
      return new Region("BUSAN", "Busan", 4500, 5000, 1600, 135, 100, 125, 20, 35, "PUBLIC_AND_PRIVATE", "Busan distance-based estimate");
    }
    if (inDaegu(lat, lng)) {
      return new Region("DAEGU", "Daegu", 4300, 4700, 1600, 133, 100, 120, 20, 35, "PUBLIC_AND_PRIVATE", "Daegu distance-based estimate");
    }
    if (inDaejeon(lat, lng)) {
      return new Region("DAEJEON", "Daejeon", 4300, 4700, 1600, 133, 100, 120, 20, 35, "PUBLIC_AND_PRIVATE", "Daejeon distance-based estimate");
    }
    if (inGwangju(lat, lng)) {
      return new Region("GWANGJU", "Gwangju", 4300, 4700, 1600, 133, 100, 120, 20, 35, "PUBLIC_AND_PRIVATE", "Gwangju distance-based estimate");
    }
    if (inUlsan(lat, lng)) {
      return new Region("ULSAN", "Ulsan", 4500, 5000, 1600, 135, 100, 125, 20, 35, "PUBLIC_AND_PRIVATE", "Ulsan distance-based estimate");
    }
    if (inJeju(lat, lng)) {
      return new Region("JEJU", "Jeju", 4300, 4800, 1600, 135, 100, 120, 25, 40, "PUBLIC_AND_PRIVATE", "Jeju distance-based estimate");
    }
    return new Region("OTHER", "Other", 4000, 4500, 1600, 140, 100, 115, 25, 45, "PUBLIC_AND_PRIVATE", "Nationwide average estimate");
  }

  private int estimateFare(
      double distanceMeters,
      int baseDistanceMeter,
      int baseFareWon,
      int incrementMeter,
      int incrementFareWon) {
    if (distanceMeters <= baseDistanceMeter) {
      return baseFareWon;
    }
    double blocks = Math.ceil((distanceMeters - baseDistanceMeter) / (double) incrementMeter);
    return baseFareWon + (int) blocks * incrementFareWon;
  }

  private boolean inSeoul(double lat, double lng) {
    return lat >= 37.413 && lat <= 37.716 && lng >= 126.734 && lng <= 127.269;
  }

  private boolean inIncheon(double lat, double lng) {
    return lat >= 37.343 && lat <= 37.589 && lng >= 126.318 && lng <= 126.831;
  }

  private boolean inBusan(double lat, double lng) {
    return lat >= 35.009 && lat <= 35.389 && lng >= 128.839 && lng <= 129.343;
  }

  private boolean inDaegu(double lat, double lng) {
    return lat >= 35.736 && lat <= 35.959 && lng >= 128.448 && lng <= 128.769;
  }

  private boolean inDaejeon(double lat, double lng) {
    return lat >= 36.241 && lat <= 36.499 && lng >= 127.243 && lng <= 127.520;
  }

  private boolean inGwangju(double lat, double lng) {
    return lat >= 35.035 && lat <= 35.261 && lng >= 126.723 && lng <= 126.969;
  }

  private boolean inUlsan(double lat, double lng) {
    return lat >= 35.379 && lat <= 35.717 && lng >= 129.151 && lng <= 129.497;
  }

  private boolean inJeju(double lat, double lng) {
    return lat >= 33.106 && lat <= 33.615 && lng >= 126.097 && lng <= 126.734;
  }

  public record Region(
      String code,
      String label,
      int baseFareWon,
      int accessibleBaseFareWon,
      int baseDistanceMeter,
      int incrementMeter,
      int incrementFareWon,
      int accessibleIncrementFareWon,
      int taxiWaitMinutes,
      int accessibleWaitMinutes,
      String providerCategory,
      String note) {}
}
