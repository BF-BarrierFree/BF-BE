package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.route.dto.TransitRouteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdsayRouteService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter ARRIVAL_TIME_FORMATTER =
      DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final WebClient webClient;
  private final TagoRouteService tagoRouteService;
  private final SeoulBusRouteService seoulBusRouteService;

  @Value("${odsay.api.base-url}")
  private String baseUrl;

  @Value("${odsay.api.api-key}")
  private String apiKey;

  public TransitRouteResponse getTransitRoute(
      double startLng, double startLat, double endLng, double endLat) {
    String rawResponse = requestTransitRoute(startLng, startLat, endLng, endLat, 0, null, 0);
    return parseTransitRoute(rawResponse);
  }

  public String testOdsayTransitRoute(
      double startLng,
      double startLat,
      double endLng,
      double endLat,
      Integer searchPathType,
      String time,
      Integer opt) {
    return requestTransitRoute(startLng, startLat, endLng, endLat, searchPathType, time, opt);
  }

  private String requestTransitRoute(
      double startLng,
      double startLat,
      double endLng,
      double endLat,
      Integer searchPathType,
      String time,
      Integer opt) {
    log.info(
        "ODsay transit route lookup start. start={},{} end={},{} type={}, time={}, opt={}, baseUrl={}, apiKeyLength={}, apiKeyFingerprint={}",
        startLng,
        startLat,
        endLng,
        endLat,
        searchPathType,
        time,
        opt,
        baseUrl,
        normalizedApiKey(apiKey).length(),
        apiKeyFingerprint(apiKey));

    URI requestUri = buildRequestUri(startLng, startLat, endLng, endLat, searchPathType, time, opt);

    String rawResponse =
        webClient
            .get()
            .uri(requestUri)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            errorBody -> {
                              log.error("ODsay client error. response={}", errorBody);
                              return Mono.error(new CustomException(ErrorCode.ODSAY_API_FAILED));
                            }))
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response -> {
                  log.error("ODsay server error.");
                  return Mono.error(new CustomException(ErrorCode.ODSAY_API_FAILED));
                })
            .bodyToMono(String.class)
            .onErrorMap(
                throwable -> {
                  if (throwable instanceof CustomException) {
                    return throwable;
                  }
                  log.error("ODsay request failed: {}", throwable.getMessage());
                  return new CustomException(ErrorCode.ODSAY_API_FAILED);
                })
            .block();

    if (rawResponse == null) {
      throw new CustomException(ErrorCode.ODSAY_API_FAILED);
    }

    return rawResponse;
  }

  private URI buildRequestUri(
      double startLng,
      double startLat,
      double endLng,
      double endLat,
      Integer searchPathType,
      String time,
      Integer opt) {
    StringBuilder requestUrl =
        new StringBuilder(baseUrl)
            .append("/v1/api/searchPubTransPathT")
            .append("?SX=")
            .append(startLng)
            .append("&SY=")
            .append(startLat)
            .append("&EX=")
            .append(endLng)
            .append("&EY=")
            .append(endLat)
            .append("&SearchType=0")
            .append("&SearchPathType=")
            .append(searchPathType != null ? searchPathType : 0)
            .append("&OPT=")
            .append(opt != null ? opt : 0);

    if (time != null && !time.isBlank()) {
      requestUrl
          .append("&time=")
          .append(UriUtils.encodeQueryParam(time.trim(), StandardCharsets.UTF_8));
    }

    requestUrl.append("&apiKey=").append(encodeApiKey(apiKey));

    return URI.create(requestUrl.toString());
  }

  private String encodeApiKey(String rawApiKey) {
    return URLEncoder.encode(normalizedApiKey(rawApiKey), StandardCharsets.UTF_8);
  }

  private String normalizedApiKey(String rawApiKey) {
    if (rawApiKey == null || rawApiKey.isBlank()) {
      throw new CustomException(ErrorCode.ODSAY_API_AUTH_FAILED);
    }

    String normalized = rawApiKey.trim();
    if ((normalized.startsWith("\"") && normalized.endsWith("\""))
        || (normalized.startsWith("'") && normalized.endsWith("'"))) {
      normalized = normalized.substring(1, normalized.length() - 1).trim();
    }

    if (normalized.contains("%")) {
      normalized = UriUtils.decode(normalized, StandardCharsets.UTF_8);
    }

    return normalized;
  }

  private String apiKeyFingerprint(String rawApiKey) {
    String normalized = normalizedApiKey(rawApiKey);
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, 8);
    } catch (NoSuchAlgorithmException e) {
      return "unknown";
    }
  }

  private TransitRouteResponse parseTransitRoute(String rawResponse) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
      JsonNode errorNode = root.path("error");
      if (!errorNode.isMissingNode() && !errorNode.isNull() && !errorNode.isEmpty()) {
        log.warn("ODsay returned error payload: {}", errorNode.toString());
        if (errorNode.toString().contains("ApiKeyAuthFailed")) {
          log.warn(
              "ODsay API authentication failed. baseUrl={}, apiKeyLength={}, apiKeyFingerprint={}",
              baseUrl,
              normalizedApiKey(apiKey).length(),
              apiKeyFingerprint(apiKey));
          throw new CustomException(ErrorCode.ODSAY_API_AUTH_FAILED);
        }
        throw new CustomException(ErrorCode.ODSAY_API_FAILED);
      }

      JsonNode pathNodes = root.at("/result/path");
      if (pathNodes.isMissingNode() || pathNodes.isNull()) {
        throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
      }

      List<TransitRouteResponse.RouteOption> routes = new ArrayList<>();
      if (pathNodes.isArray()) {
        for (JsonNode pathNode : pathNodes) {
          TransitRouteResponse.RouteOption routeOption = parseRouteOption(pathNode);
          if (routeOption != null) {
            routes.add(routeOption);
          }
        }
      } else {
        TransitRouteResponse.RouteOption routeOption = parseRouteOption(pathNodes);
        if (routeOption != null) {
          routes.add(routeOption);
        }
      }

      if (routes.isEmpty()) {
        throw new CustomException(ErrorCode.ROUTE_NOT_FOUND);
      }

      TransitRouteResponse.RouteOption primaryRoute = routes.get(0);
      return new TransitRouteResponse(
          routes.size(), primaryRoute.totalTimeMinute(), primaryRoute.totalDistanceMeter(), routes);
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.warn("ODsay transit response parsing failed.", e);
      throw new CustomException(ErrorCode.ODSAY_API_FAILED);
    }
  }

  private TransitRouteResponse.RouteOption parseRouteOption(JsonNode pathNode) {
    if (pathNode == null || pathNode.isMissingNode() || pathNode.isNull()) {
      return null;
    }

    JsonNode info = pathNode.path("info");
    List<TransitRouteResponse.Segment> segments = new ArrayList<>();
    JsonNode subPaths = pathNode.path("subPath");
    if (subPaths.isArray()) {
      for (JsonNode subPath : subPaths) {
        segments.add(parseSegment(subPath));
      }
    }

    Integer transferCount = intValue(info, "transferCount");
    if (transferCount == null) {
      transferCount = calculateTransferCount(segments);
    }

    Integer totalTimeMinute = intValue(info, "totalTime");

    return new TransitRouteResponse.RouteOption(
        intValue(pathNode, "pathType"),
        totalTimeMinute,
        intValue(info, "totalDistance"),
        intValue(info, "totalWalk"),
        intValue(info, "payment"),
        transferCount,
        textValue(info, "lastEndStation"),
        estimatedArrivalTime(totalTimeMinute),
        collectPathCoordinates(segments),
        segments);
  }

  private TransitRouteResponse.Segment parseSegment(JsonNode subPath) {
    if (subPath == null || subPath.isMissingNode() || subPath.isNull()) {
      return new TransitRouteResponse.Segment(
          null, null, null, null, null, null, null, null, null, null, List.of(), List.of(),
          List.of(), List.of(), false, List.of(), List.of());
    }

    List<String> laneNames = new ArrayList<>();
    List<TransitRouteResponse.Lane> laneResponses = new ArrayList<>();
    JsonNode lanes = subPath.path("lane");
    if (lanes.isArray()) {
      for (JsonNode lane : lanes) {
        String name = textValue(lane, "name");
        if (name != null && !name.isBlank()) {
          laneNames.add(name);
        }
        laneResponses.add(
            new TransitRouteResponse.Lane(
                name,
                textValue(lane, "busNo"),
                intValue(lane, "type"),
                busTypeName(intValue(lane, "type")),
                textValue(lane, "busID"),
                intValue(lane, "subwayCode"),
                textValue(lane, "subwayCityCode")));
      }
    }

    List<TransitRouteResponse.Stop> passStops = parsePassStops(subPath.path("passStopList"));
    List<TransitRouteResponse.Point> pathCoordinates =
        collectSegmentPathCoordinates(subPath, passStops);
    TagoRouteService.RealtimeBusSnapshot realtimeSnapshot =
        fetchRealtimeBusSnapshot(subPath, laneResponses);

    return new TransitRouteResponse.Segment(
        intValue(subPath, "trafficType"),
        textValue(subPath, "startName"),
        textValue(subPath, "endName"),
        intValue(subPath, "distance"),
        intValue(subPath, "sectionTime"),
        intValue(subPath, "stationCount"),
        coordinateValue(subPath, "startX"),
        coordinateValue(subPath, "startY"),
        coordinateValue(subPath, "endX"),
        coordinateValue(subPath, "endY"),
        laneNames,
        laneResponses,
        passStops,
        pathCoordinates,
        !realtimeSnapshot.arrivals().isEmpty() || !realtimeSnapshot.locations().isEmpty(),
        realtimeSnapshot.arrivals(),
        realtimeSnapshot.locations());
  }

  private TagoRouteService.RealtimeBusSnapshot fetchRealtimeBusSnapshot(
      JsonNode subPath, List<TransitRouteResponse.Lane> lanes) {
    if (intValue(subPath, "trafficType") == null || intValue(subPath, "trafficType") != 2) {
      return TagoRouteService.RealtimeBusSnapshot.empty();
    }
    if (lanes.isEmpty()) {
      return TagoRouteService.RealtimeBusSnapshot.empty();
    }

    String busNo = lanes.get(0).busNo();
    if (busNo == null || busNo.isBlank()) {
      busNo = lanes.get(0).name();
    }

    Double startLat = coordinateValue(subPath, "startY");
    Double startLng = coordinateValue(subPath, "startX");
    TagoRouteService.RealtimeBusSnapshot tagoSnapshot =
        tagoRouteService.getRealtimeBusSnapshot(startLat, startLng, busNo);
    if (!tagoSnapshot.arrivals().isEmpty() || !tagoSnapshot.locations().isEmpty()) {
      return tagoSnapshot;
    }

    return seoulBusRouteService.getRealtimeBusSnapshot(startLat, startLng, busNo);
  }

  private String busTypeName(Integer type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case 1 -> "일반";
      case 2 -> "좌석";
      case 3 -> "마을버스";
      case 4 -> "직행좌석";
      case 5 -> "공항버스";
      case 6 -> "간선급행";
      case 10 -> "외곽";
      case 11 -> "간선";
      case 12 -> "지선";
      case 13 -> "순환";
      case 14 -> "광역";
      case 15 -> "급행";
      case 16 -> "관광버스";
      case 20 -> "농어촌버스";
      case 22 -> "경기도 시외형버스";
      case 26 -> "급행간선";
      case 30 -> "한강버스";
      default -> "기타";
    };
  }

  private Integer calculateTransferCount(List<TransitRouteResponse.Segment> segments) {
    long transitSegmentCount =
        segments.stream()
            .filter(segment -> segment.trafficType() != null && segment.trafficType() != 3)
            .count();
    return Math.max(0, (int) transitSegmentCount - 1);
  }

  private String estimatedArrivalTime(Integer totalTimeMinute) {
    if (totalTimeMinute == null) {
      return null;
    }
    return ZonedDateTime.now(SEOUL_ZONE_ID)
        .plusMinutes(totalTimeMinute)
        .format(ARRIVAL_TIME_FORMATTER);
  }

  private List<TransitRouteResponse.Stop> parsePassStops(JsonNode passStopList) {
    JsonNode stations = passStopList.path("stations");
    if (!stations.isArray()) {
      return List.of();
    }

    List<TransitRouteResponse.Stop> stops = new ArrayList<>();
    for (JsonNode station : stations) {
      stops.add(
          new TransitRouteResponse.Stop(
              intValue(station, "index"),
              textValue(station, "stationID"),
              textValue(station, "stationName"),
              coordinateValue(station, "x"),
              coordinateValue(station, "y")));
    }
    return stops;
  }

  private List<TransitRouteResponse.Point> collectSegmentPathCoordinates(
      JsonNode subPath, List<TransitRouteResponse.Stop> passStops) {
    List<TransitRouteResponse.Point> points = new ArrayList<>();
    addPoint(points, coordinateValue(subPath, "startX"), coordinateValue(subPath, "startY"));
    for (TransitRouteResponse.Stop stop : passStops) {
      addPoint(points, stop.lng(), stop.lat());
    }
    addPoint(points, coordinateValue(subPath, "endX"), coordinateValue(subPath, "endY"));
    return points;
  }

  private List<TransitRouteResponse.Point> collectPathCoordinates(
      List<TransitRouteResponse.Segment> segments) {
    List<TransitRouteResponse.Point> points = new ArrayList<>();
    for (TransitRouteResponse.Segment segment : segments) {
      for (TransitRouteResponse.Point point : segment.pathCoordinates()) {
        addPoint(points, point.lng(), point.lat());
      }
    }
    return points;
  }

  private void addPoint(List<TransitRouteResponse.Point> points, Double lng, Double lat) {
    if (lng == null || lat == null) {
      return;
    }
    if (!points.isEmpty()) {
      TransitRouteResponse.Point lastPoint = points.get(points.size() - 1);
      if (lng.equals(lastPoint.lng()) && lat.equals(lastPoint.lat())) {
        return;
      }
    }
    points.add(new TransitRouteResponse.Point(lng, lat));
  }

  private Integer intValue(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    JsonNode field = node.get(fieldName);
    return field == null || field.isNull() ? null : field.asInt();
  }

  private String textValue(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    JsonNode field = node.get(fieldName);
    return field == null || field.isNull() ? null : field.asText();
  }

  private Double coordinateValue(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    JsonNode field = node.get(fieldName);
    if (field == null || field.isNull()) {
      return null;
    }
    if (field.isNumber()) {
      return field.asDouble();
    }
    String value = field.asText();
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
