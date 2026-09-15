package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.route.dto.TransitRouteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class TagoRouteService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient tagoWebClient;

  @Value("${tago.api.base-url}")
  private String baseUrl;

  @Value("${tago.api.station-base-url}")
  private String stationBaseUrl;

  @Value("${tago.api.route-base-url}")
  private String routeBaseUrl;

  @Value("${tago.api.location-base-url}")
  private String locationBaseUrl;

  @Value("${tago.api.service-key}")
  private String serviceKey;

  // 공공데이터포털(TAGO) 전용으로 만든 tagoWebClient 빈을 명시적으로 주입
  public TagoRouteService(@Qualifier("tagoWebClient") WebClient tagoWebClient) {
    this.tagoWebClient = tagoWebClient;
  }

  public record RealtimeBusSnapshot(
      List<TransitRouteResponse.RealtimeArrival> arrivals,
      List<TransitRouteResponse.BusLocation> locations) {
    public static RealtimeBusSnapshot empty() {
      return new RealtimeBusSnapshot(List.of(), List.of());
    }
  }

  /**
   * [테스트용 1] 공공데이터포털(TAGO) 연동 테스트: 도시코드 목록 조회 연결 및 인증 성공 여부를 직관적으로 확인하기 위해 원본 String 형태로 데이터를
   * 반환합니다.
   */
  public String testTagoCityCodeConnection() {
    log.info("TAGO 도시코드 목록 조회 연동 테스트 시작");

    String requestUrl =
        baseUrl + "/getCtyCodeList?serviceKey=" + encodedServiceKey() + "&_type=json";

    String rawResponse =
        tagoWebClient
            .get()
            .uri(requestUrl)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            errorBody -> {
                              log.error("TAGO 클라이언트 에러(4xx) 발생. Response: {}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("TAGO 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                })
            .bodyToMono(String.class)
            .onErrorMap(
                throwable -> {
                  log.error("TAGO API 호출 중 네트워크/타임아웃 에러 발생: {}", throwable.getMessage());
                  return new CustomException(ErrorCode.TAGO_API_FAILED);
                })
            .block();

    if (rawResponse == null) {
      log.error("TAGO 응답이 null 입니다.");
      throw new CustomException(ErrorCode.TAGO_API_FAILED);
    }

    return rawResponse;
  }

  /**
   * [테스트용 2] 공공데이터포털(TAGO) 연동 테스트: 정류소별 버스 도착예정정보 응답 데이터 중 'vehicletp' 필드가 '저상버스'인지 확인하기 위한 테스트입니다.
   */
  public String testTagoBusArrivalInfo(int cityCode, String nodeId) {
    log.info("TAGO 버스 도착예정정보(저상버스) 연동 테스트 시작 - cityCode: {}, nodeId: {}", cityCode, nodeId);

    String requestUrl =
        baseUrl
            + "/getSttnAcctoArvlPrearngeInfoList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&cityCode="
            + cityCode
            + "&nodeId="
            + nodeId
            + "&numOfRows=10"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient
            .get()
            .uri(requestUrl)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            errorBody -> {
                              log.error("TAGO 버스 도착정보 클라이언트 에러(4xx) 발생. Response: {}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("TAGO 버스 도착정보 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                })
            .bodyToMono(String.class)
            .onErrorMap(
                throwable -> {
                  log.error("TAGO API 호출 중 네트워크/타임아웃 에러 발생: {}", throwable.getMessage());
                  return new CustomException(ErrorCode.TAGO_API_FAILED);
                })
            .block();

    if (rawResponse == null) {
      log.error("TAGO 버스 도착정보 응답이 null 입니다.");
      throw new CustomException(ErrorCode.TAGO_API_FAILED);
    }

    return rawResponse;
  }

  /**
   * [테스트용 3] 공공데이터포털(TAGO) 연동 테스트: 좌표기반 근접 정류소 목록 조회 ODsay에서 얻은 위경도를 통해 TAGO의 정류소 ID(nodeId)와
   * 도시코드(cityCode)를 알아냅니다.
   */
  public String testTagoNearbyStation(double lat, double lng) {
    log.info("TAGO 근접 정류소 조회 연동 테스트 시작 - lat: {}, lng: {}", lat, lng);

    String requestUrl =
        stationBaseUrl
            + "/getCrdntPrxmtSttnList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&gpsLati="
            + lat
            + "&gpsLong="
            + lng
            + "&numOfRows=10"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient
            .get()
            .uri(requestUrl)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            errorBody -> {
                              log.error("TAGO 근접 정류소 조회 클라이언트 에러(4xx) 발생. Response: {}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("TAGO 근접 정류소 조회 외부 서버 에러(5xx) 발생.");
                  return Mono.error(new CustomException(ErrorCode.TAGO_API_FAILED));
                })
            .bodyToMono(String.class)
            .onErrorMap(
                throwable -> {
                  log.error("TAGO API 호출 중 네트워크/타임아웃 에러 발생: {}", throwable.getMessage());
                  return new CustomException(ErrorCode.TAGO_API_FAILED);
                })
            .block();

    if (rawResponse == null) {
      log.error("TAGO 근접 정류소 조회 응답이 null 입니다.");
      throw new CustomException(ErrorCode.TAGO_API_FAILED);
    }

    return rawResponse;
  }

  public List<TransitRouteResponse.RealtimeArrival> getRealtimeBusArrivals(
      Double lat, Double lng, String busNo) {
    return getRealtimeBusSnapshot(lat, lng, busNo).arrivals();
  }

  public RealtimeBusSnapshot getRealtimeBusSnapshot(Double lat, Double lng, String busNo) {
    if (lat == null || lng == null || busNo == null || busNo.isBlank()) {
      return RealtimeBusSnapshot.empty();
    }

    try {
      List<JsonNode> stations = findNearbyStations(lat, lng);
      if (stations.isEmpty()) {
        return RealtimeBusSnapshot.empty();
      }

      String fallbackRouteId = null;
      String fallbackCityCode = null;
      for (JsonNode station : stations) {
        String cityCode = textValue(station, "citycode");
        String nodeId = textValue(station, "nodeid");
        if (cityCode == null || nodeId == null) {
          continue;
        }

        List<RouteCandidate> routeCandidates = findRouteCandidatesByRouteNo(cityCode, busNo);
        for (RouteCandidate routeCandidate : routeCandidates) {
          String routeId = routeCandidate.routeId();
          String routeNo = routeCandidate.routeNo();
          if (fallbackRouteId == null) {
            fallbackRouteId = routeId;
            fallbackCityCode = cityCode;
          }

          JsonNode routeStation = findNearestRouteStation(lat, lng, cityCode, routeId);
          String arrivalNodeId = routeStation == null ? nodeId : textValue(routeStation, "nodeid");
          if (arrivalNodeId == null) {
            arrivalNodeId = nodeId;
          }

          List<JsonNode> arrivalItems =
              findBusArrivalItems(cityCode, arrivalNodeId, routeId, busNo);
          if (arrivalItems.isEmpty()) {
            continue;
          }

          List<TransitRouteResponse.RealtimeArrival> arrivals =
              arrivalItems.stream().map(this::toRealtimeArrival).toList();
          List<TransitRouteResponse.BusLocation> locations =
              findBusLocations(cityCode, routeId, routeNo);

          return new RealtimeBusSnapshot(arrivals, locations);
        }

        List<JsonNode> arrivalItems = findBusArrivalItems(cityCode, nodeId, busNo);
        if (!arrivalItems.isEmpty()) {
          String routeId = firstTextValue(arrivalItems, "routeid");
          List<TransitRouteResponse.RealtimeArrival> arrivals =
              arrivalItems.stream().map(this::toRealtimeArrival).toList();
          List<TransitRouteResponse.BusLocation> locations =
              routeId == null ? List.of() : findBusLocations(cityCode, routeId, busNo);

          return new RealtimeBusSnapshot(arrivals, locations);
        }
      }

      if (fallbackRouteId != null) {
        return new RealtimeBusSnapshot(
            List.of(), findBusLocations(fallbackCityCode, fallbackRouteId, busNo));
      }

      return RealtimeBusSnapshot.empty();
    } catch (Exception e) {
      log.warn(
          "TAGO realtime bus information lookup failed. lat={}, lng={}, busNo={}, reason={}",
          lat,
          lng,
          busNo,
          e.getMessage());
      return RealtimeBusSnapshot.empty();
    }
  }

  private List<JsonNode> findNearbyStations(double lat, double lng) throws Exception {
    String requestUrl =
        stationBaseUrl
            + "/getCrdntPrxmtSttnList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&gpsLati="
            + lat
            + "&gpsLong="
            + lng
            + "&numOfRows=5"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient.get().uri(requestUrl).retrieve().bodyToMono(String.class).block();
    JsonNode items = itemsNode(rawResponse);
    if (items == null || items.isMissingNode() || items.isNull()) {
      return List.of();
    }

    List<JsonNode> stations = new ArrayList<>();
    if (items.isArray()) {
      items.forEach(stations::add);
    } else {
      stations.add(items);
    }
    return stations;
  }

  private List<TransitRouteResponse.RealtimeArrival> findBusArrivals(
      String cityCode, String nodeId, String busNo) throws Exception {
    return findBusArrivalItems(cityCode, nodeId, busNo).stream()
        .map(this::toRealtimeArrival)
        .toList();
  }

  private List<JsonNode> findBusArrivalItems(String cityCode, String nodeId, String busNo)
      throws Exception {
    String requestUrl =
        baseUrl
            + "/getSttnAcctoArvlPrearngeInfoList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&cityCode="
            + cityCode
            + "&nodeId="
            + nodeId
            + "&numOfRows=20"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient.get().uri(requestUrl).retrieve().bodyToMono(String.class).block();
    JsonNode items = itemsNode(rawResponse);
    if (items == null || items.isMissingNode() || items.isNull()) {
      return List.of();
    }

    List<JsonNode> arrivals = new ArrayList<>();
    if (items.isArray()) {
      for (JsonNode item : items) {
        addIfMatchesBusNo(arrivals, item, busNo);
      }
    } else {
      addIfMatchesBusNo(arrivals, items, busNo);
    }
    return arrivals;
  }

  private List<JsonNode> findBusArrivalItems(
      String cityCode, String nodeId, String routeId, String busNo) throws Exception {
    String requestUrl =
        baseUrl
            + "/getSttnAcctoSpcifyRouteBusArvlPrearngeInfoList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&cityCode="
            + cityCode
            + "&nodeId="
            + nodeId
            + "&routeId="
            + routeId
            + "&numOfRows=20"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient.get().uri(requestUrl).retrieve().bodyToMono(String.class).block();
    JsonNode items = itemsNode(rawResponse);
    if (items == null || items.isMissingNode() || items.isNull()) {
      return findBusArrivalItems(cityCode, nodeId, busNo);
    }

    List<JsonNode> arrivals = new ArrayList<>();
    if (items.isArray()) {
      items.forEach(arrivals::add);
    } else {
      arrivals.add(items);
    }
    return arrivals.isEmpty() ? findBusArrivalItems(cityCode, nodeId, busNo) : arrivals;
  }

  private List<TransitRouteResponse.BusLocation> findBusLocations(
      String cityCode, String routeId, String busNo) throws Exception {
    String requestUrl =
        locationBaseUrl
            + "/getRouteAcctoBusLcList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&cityCode="
            + cityCode
            + "&routeId="
            + routeId
            + "&numOfRows=50"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient.get().uri(requestUrl).retrieve().bodyToMono(String.class).block();
    JsonNode items = itemsNode(rawResponse);
    if (items == null || items.isMissingNode() || items.isNull()) {
      return List.of();
    }

    List<TransitRouteResponse.BusLocation> locations = new ArrayList<>();
    if (items.isArray()) {
      for (JsonNode item : items) {
        addBusLocation(locations, item, busNo);
      }
    } else {
      addBusLocation(locations, items, busNo);
    }
    return locations;
  }

  private String findRouteIdByRouteNo(String cityCode, String busNo) throws Exception {
    List<RouteCandidate> routeCandidates = findRouteCandidatesByRouteNo(cityCode, busNo);
    return routeCandidates.isEmpty() ? null : routeCandidates.get(0).routeId();
  }

  private List<RouteCandidate> findRouteCandidatesByRouteNo(String cityCode, String busNo)
      throws Exception {
    String requestUrl =
        routeBaseUrl
            + "/getRouteNoList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&cityCode="
            + cityCode
            + "&routeNo="
            + URLEncoder.encode(busNo, StandardCharsets.UTF_8)
            + "&numOfRows=20"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient.get().uri(requestUrl).retrieve().bodyToMono(String.class).block();
    JsonNode items = itemsNode(rawResponse);
    if (items == null || items.isMissingNode() || items.isNull()) {
      return List.of();
    }

    List<RouteCandidate> routeCandidates = new ArrayList<>();
    if (items.isArray()) {
      for (JsonNode item : items) {
        String routeId = textValue(item, "routeid");
        String routeNo = textValue(item, "routeno");
        if (routeId == null || routeId.isBlank()) {
          continue;
        }
        if (busNoMatches(routeNo, busNo)) {
          routeCandidates.add(new RouteCandidate(routeId, firstNonBlank(routeNo, busNo)));
        }
      }
      return routeCandidates;
    }

    String routeId = textValue(items, "routeid");
    String routeNo = textValue(items, "routeno");
    return routeId == null || routeId.isBlank() || !busNoMatches(routeNo, busNo)
        ? List.of()
        : List.of(new RouteCandidate(routeId, firstNonBlank(routeNo, busNo)));
  }

  private JsonNode findNearestRouteStation(Double lat, Double lng, String cityCode, String routeId)
      throws Exception {
    if (lat == null || lng == null) {
      return null;
    }

    String requestUrl =
        routeBaseUrl
            + "/getRouteAcctoThrghSttnList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&cityCode="
            + cityCode
            + "&routeId="
            + routeId
            + "&numOfRows=200"
            + "&pageNo=1"
            + "&_type=json";

    String rawResponse =
        tagoWebClient.get().uri(requestUrl).retrieve().bodyToMono(String.class).block();
    JsonNode items = itemsNode(rawResponse);
    if (items == null || items.isMissingNode() || items.isNull()) {
      return null;
    }

    JsonNode nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    if (items.isArray()) {
      for (JsonNode item : items) {
        double distance =
            squaredDistance(lat, lng, doubleValue(item, "gpslati"), doubleValue(item, "gpslong"));
        if (distance < nearestDistance) {
          nearestDistance = distance;
          nearest = item;
        }
      }
    } else {
      nearest = items;
    }
    return nearest;
  }

  private void addIfMatchesBusNo(List<JsonNode> arrivals, JsonNode item, String busNo) {
    String routeNo = textValue(item, "routeno");
    if (!busNoMatches(routeNo, busNo)) {
      return;
    }
    arrivals.add(item);
  }

  private TransitRouteResponse.RealtimeArrival toRealtimeArrival(JsonNode item) {
    String routeNo = textValue(item, "routeno");
    Integer arrivalSeconds = intValue(item, "arrtime");
    Integer arrivalMinutes =
        arrivalSeconds == null ? null : Math.max(0, (int) Math.ceil(arrivalSeconds / 60.0));
    return new TransitRouteResponse.RealtimeArrival(
        routeNo,
        arrivalMinutes,
        arrivalMinutes == null ? null : arrivalMinutes + "분 후 도착",
        intValue(item, "arrprevstationcnt"),
        textValue(item, "vehicletp"),
        null);
  }

  private void addBusLocation(
      List<TransitRouteResponse.BusLocation> locations, JsonNode item, String busNo) {
    Double lat = doubleValue(item, "gpslati");
    Double lng = doubleValue(item, "gpslong");
    if (lat == null || lng == null) {
      return;
    }
    locations.add(
        new TransitRouteResponse.BusLocation(
            busNo, lng, lat, firstNonBlank(textValue(item, "nodenm"), textValue(item, "nodeid"))));
  }

  private JsonNode itemsNode(String rawResponse) throws Exception {
    if (rawResponse == null || rawResponse.isBlank()) {
      return null;
    }
    JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
    return root.path("response").path("body").path("items").path("item");
  }

  private String textValue(JsonNode node, String fieldName) {
    JsonNode field = fieldValue(node, fieldName);
    return field == null || field.isNull() ? null : field.asText();
  }

  private Integer intValue(JsonNode node, String fieldName) {
    JsonNode field = fieldValue(node, fieldName);
    if (field == null || field.isNull()) {
      return null;
    }
    if (field.isNumber()) {
      return field.asInt();
    }
    try {
      return Integer.parseInt(field.asText());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Double doubleValue(JsonNode node, String fieldName) {
    JsonNode field = fieldValue(node, fieldName);
    if (field == null || field.isNull()) {
      return null;
    }
    if (field.isNumber()) {
      return field.asDouble();
    }
    try {
      return Double.parseDouble(field.asText());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String firstTextValue(List<JsonNode> items, String fieldName) {
    for (JsonNode item : items) {
      String value = textValue(item, fieldName);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second == null || second.isBlank() ? null : second;
  }

  private double squaredDistance(
      Double baseLat, Double baseLng, Double targetLat, Double targetLng) {
    if (targetLat == null || targetLng == null) {
      return Double.MAX_VALUE;
    }
    double latDiff = baseLat - targetLat;
    double lngDiff = baseLng - targetLng;
    return latDiff * latDiff + lngDiff * lngDiff;
  }

  private JsonNode fieldValue(JsonNode node, String fieldName) {
    if (node == null || fieldName == null) {
      return null;
    }
    JsonNode exact = node.get(fieldName);
    if (exact != null) {
      return exact;
    }
    for (Map.Entry<String, JsonNode> field : node.properties()) {
      if (field.getKey().equalsIgnoreCase(fieldName)) {
        return field.getValue();
      }
    }
    return null;
  }

  private boolean busNoMatches(String candidate, String requested) {
    String normalizedCandidate = normalizeBusNo(candidate);
    String normalizedRequested = normalizeBusNo(requested);
    return normalizedCandidate != null
        && normalizedRequested != null
        && normalizedCandidate.equals(normalizedRequested);
  }

  private String normalizeBusNo(String busNo) {
    if (busNo == null || busNo.isBlank()) {
      return null;
    }
    StringBuilder normalized = new StringBuilder();
    for (int i = 0; i < busNo.length(); i++) {
      char ch = busNo.charAt(i);
      if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
        normalized.append(Character.toLowerCase(ch));
      }
    }
    return normalized.isEmpty() ? null : normalized.toString();
  }

  private record RouteCandidate(String routeId, String routeNo) {}

  private String encodedServiceKey() {
    String key = serviceKey == null ? "" : serviceKey.trim();
    if (key.contains("%")) {
      key = URLDecoder.decode(key, StandardCharsets.UTF_8);
    }
    return URLEncoder.encode(key, StandardCharsets.UTF_8);
  }
}
