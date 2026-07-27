package com.barrierfree.bf.route.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.route.dto.TransitRouteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdsayRouteService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;

  @Value("${odsay.api.base-url}")
  private String baseUrl;

  @Value("${odsay.api.api-key}")
  private String apiKey;

  public TransitRouteResponse getTransitRoute(
      double startLng, double startLat, double endLng, double endLat) {
    String rawResponse = requestTransitRoute(startLng, startLat, endLng, endLat);
    return parseTransitRoute(rawResponse);
  }

  public String testOdsayTransitRoute(
      double startLng, double startLat, double endLng, double endLat) {
    return requestTransitRoute(startLng, startLat, endLng, endLat);
  }

  private String requestTransitRoute(
      double startLng, double startLat, double endLng, double endLat) {
    log.info(
        "ODsay transit route lookup start. start={},{} end={},{}",
        startLng,
        startLat,
        endLng,
        endLat);

    String requestUrl =
        baseUrl
            + "/v1/api/searchPubTransPathT"
            + "?apiKey="
            + apiKey
            + "&SX="
            + startLng
            + "&SY="
            + startLat
            + "&EX="
            + endLng
            + "&EY="
            + endLat
            + "&OPT=0"
            + "&SearchType=0"
            + "&SearchPathType=0";

    String rawResponse =
        webClient
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
                  log.error("ODsay request failed: {}", throwable.getMessage());
                  return new CustomException(ErrorCode.ODSAY_API_FAILED);
                })
            .block();

    if (rawResponse == null) {
      throw new CustomException(ErrorCode.ODSAY_API_FAILED);
    }

    return rawResponse;
  }

  private TransitRouteResponse parseTransitRoute(String rawResponse) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(rawResponse);
      JsonNode errorNode = root.path("error");
      if (!errorNode.isMissingNode() && !errorNode.isNull() && !errorNode.isEmpty()) {
        log.warn("ODsay returned error payload: {}", errorNode.toString());
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

      return new TransitRouteResponse(routes.size(), routes);
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

    return new TransitRouteResponse.RouteOption(
        intValue(pathNode, "pathType"),
        intValue(info, "totalTime"),
        intValue(info, "totalDistance"),
        intValue(info, "payment"),
        intValue(info, "transferCount"),
        segments);
  }

  private TransitRouteResponse.Segment parseSegment(JsonNode subPath) {
    if (subPath == null || subPath.isMissingNode() || subPath.isNull()) {
      return new TransitRouteResponse.Segment(null, null, null, null, null, List.of());
    }

    List<String> laneNames = new ArrayList<>();
    JsonNode lanes = subPath.path("lane");
    if (lanes.isArray()) {
      for (JsonNode lane : lanes) {
        String name = textValue(lane, "name");
        if (name != null && !name.isBlank()) {
          laneNames.add(name);
        }
      }
    }

    return new TransitRouteResponse.Segment(
        intValue(subPath, "trafficType"),
        textValue(subPath, "startName"),
        textValue(subPath, "endName"),
        intValue(subPath, "distance"),
        intValue(subPath, "sectionTime"),
        laneNames);
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
}
