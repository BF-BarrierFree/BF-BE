package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.route.dto.OrsGeoJsonResponse;
import com.barrierfree.bf.route.dto.OrsRouteRequest;
import com.barrierfree.bf.route.dto.VehicleRouteResponse;
import com.barrierfree.bf.route.dto.WalkingRouteResponse;
import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrsRouteService {

  private static final String WHEELCHAIR_PROFILE = "wheelchair";
  private static final String WALKING_PROFILE = "foot-walking";
  private static final String DRIVING_PROFILE = "driving-car";

  private final WebClient webClient;
  private final RoutePricingService routePricingService;

  @Value("${openrouteservice.base-url}")
  private String baseUrl;

  @Value("${openrouteservice.api-key}")
  private String apiKey;

  @Cacheable(
      value = "ors_routes",
      key = "'wheelchair_' + #startLng + '_' + #startLat + '_' + #endLng + '_' + #endLat")
  public WheelchairRouteResponse getWheelchairRoute(
      double startLng, double startLat, double endLng, double endLat) {
    return WheelchairRouteResponse.from(
        fetchRoute(
            WHEELCHAIR_PROFILE,
            OrsRouteRequest.createWheelchair(startLng, startLat, endLng, endLat)));
  }

  @Cacheable(
      value = "ors_routes",
      key = "'walk_' + #startLng + '_' + #startLat + '_' + #endLng + '_' + #endLat")
  public WalkingRouteResponse getAccessibleWalkingRoute(
      double startLng, double startLat, double endLng, double endLat) {
    OrsGeoJsonResponse rawResponse =
        fetchRoute(
            WHEELCHAIR_PROFILE,
            OrsRouteRequest.createWheelchair(startLng, startLat, endLng, endLat));
    WheelchairRouteResponse route = WheelchairRouteResponse.from(rawResponse);
    return WalkingRouteResponse.from(route, true, "WHEELCHAIR_ACCESSIBLE");
  }

  @Cacheable(
      value = "ors_routes",
      key = "'vehicle_' + #startLng + '_' + #startLat + '_' + #endLng + '_' + #endLat")
  public VehicleRouteResponse getVehicleRoute(
      double startLng, double startLat, double endLng, double endLat) {
    OrsGeoJsonResponse rawResponse =
        fetchRoute(
            DRIVING_PROFILE, OrsRouteRequest.createDriving(startLng, startLat, endLng, endLat));
    WheelchairRouteResponse route = WheelchairRouteResponse.from(rawResponse);
    RoutePricingService.Region region = routePricingService.classify(startLat, startLng);
    return VehicleRouteResponse.from(
        route,
        region,
        routePricingService.estimateTaxi(route.totalDistanceMeter(), region),
        routePricingService.estimateAccessibleTaxi(route.totalDistanceMeter(), region));
  }

  private OrsGeoJsonResponse fetchRoute(String profile, OrsRouteRequest requestPayload) {
    OrsGeoJsonResponse rawResponse =
        webClient
            .mutate()
            .baseUrl(baseUrl)
            .build()
            .post()
            .uri("/v2/directions/" + profile + "/geojson")
            .header(HttpHeaders.AUTHORIZATION, apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestPayload)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            errorBody -> {
                              log.error(
                                  "ORS client error. profile={}, response={}", profile, errorBody);
                              if (errorBody.contains("\"code\":2009")
                                  || errorBody.contains("\"code\":2010")
                                  || errorBody.contains("could not be found")) {
                                return Mono.error(new CustomException(ErrorCode.ROUTE_NOT_FOUND));
                              }
                              if (errorBody.contains("Invalid coordinates")) {
                                return Mono.error(
                                    new CustomException(ErrorCode.INVALID_ROUTE_COORDINATES));
                              }
                              return Mono.error(new CustomException(ErrorCode.INVALID_INPUT_VALUE));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("ORS server error. profile={}", profile);
                  return Mono.error(new CustomException(ErrorCode.EXTERNAL_ROUTING_FAILED));
                })
            .bodyToMono(OrsGeoJsonResponse.class)
            .block();

    if (rawResponse == null || rawResponse.features() == null || rawResponse.features().isEmpty()) {
      throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
    }

    return rawResponse;
  }
}
