package com.barrierfree.bf.route.service;

import com.barrierfree.bf.route.dto.TransitRouteResponse;
import java.io.StringReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Slf4j
@Service
public class SeoulBusRouteService {

  private final WebClient webClient;

  @Value("${seoul.bus.api.base-url}")
  private String baseUrl;

  @Value("${seoul.bus.api.service-key}")
  private String serviceKey;

  public SeoulBusRouteService(@Qualifier("tagoWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public TagoRouteService.RealtimeBusSnapshot getRealtimeBusSnapshot(
      Double lat, Double lng, String busNo) {
    if (lat == null || lng == null || busNo == null || busNo.isBlank()) {
      return TagoRouteService.RealtimeBusSnapshot.empty();
    }

    try {
      RouteCandidate route = findRoute(busNo);
      if (route == null) {
        return TagoRouteService.RealtimeBusSnapshot.empty();
      }

      List<RouteStation> routeStations = findRouteStations(route.busRouteId());
      RouteStation station = findNearestStation(lat, lng, routeStations);
      if (station == null) {
        return new TagoRouteService.RealtimeBusSnapshot(
            List.of(), findBusLocations(route, routeStations));
      }

      List<TransitRouteResponse.RealtimeArrival> arrivals = findArrivals(route, station);
      List<TransitRouteResponse.BusLocation> locations = findBusLocations(route, routeStations);
      return new TagoRouteService.RealtimeBusSnapshot(arrivals, locations);
    } catch (Exception e) {
      log.warn(
          "Seoul bus realtime lookup failed. lat={}, lng={}, busNo={}, reason={}",
          lat,
          lng,
          busNo,
          e.getMessage());
      return TagoRouteService.RealtimeBusSnapshot.empty();
    }
  }

  private RouteCandidate findRoute(String busNo) throws Exception {
    String requestUrl =
        baseUrl
            + "/busRouteInfo/getBusRouteList"
            + "?serviceKey="
            + encodedServiceKey()
            + "&strSrch="
            + URLEncoder.encode(busNo, StandardCharsets.UTF_8);

    Document document = requestXml(requestUrl);
    logResponseSummary(document, "getBusRouteList", busNo);
    List<Element> items = itemElements(document);
    for (Element item : items) {
      String routeName = text(item, "busRouteNm");
      if (busNoMatches(routeName, busNo)) {
        return new RouteCandidate(text(item, "busRouteId"), routeName);
      }
    }
    return null;
  }

  private List<RouteStation> findRouteStations(String busRouteId) throws Exception {
    if (busRouteId == null || busRouteId.isBlank()) {
      return List.of();
    }

    String requestUrl =
        baseUrl
            + "/busRouteInfo/getStaionByRoute"
            + "?serviceKey="
            + encodedServiceKey()
            + "&busRouteId="
            + busRouteId;

    Document document = requestXml(requestUrl);
    logResponseSummary(document, "getStaionByRoute", busRouteId);
    List<RouteStation> stations = new ArrayList<>();
    for (Element item : itemElements(document)) {
      String stationId = text(item, "station");
      String sequence = text(item, "seq");
      Double lng = doubleValue(firstNonBlank(text(item, "gpsX"), text(item, "posX")));
      Double lat = doubleValue(firstNonBlank(text(item, "gpsY"), text(item, "posY")));
      if (stationId == null || sequence == null) {
        continue;
      }
      stations.add(new RouteStation(stationId, text(item, "stationNm"), sequence, lng, lat));
    }
    return stations;
  }

  private RouteStation findNearestStation(
      Double lat, Double lng, List<RouteStation> routeStations) {
    return routeStations.stream()
        .filter(station -> station.lat() != null && station.lng() != null)
        .min(
            Comparator.comparingDouble(
                station -> squaredDistance(lat, lng, station.lat(), station.lng())))
        .orElse(null);
  }

  private List<TransitRouteResponse.RealtimeArrival> findArrivals(
      RouteCandidate route, RouteStation station) throws Exception {
    String requestUrl =
        baseUrl
            + "/arrive/getArrInfoByRoute"
            + "?serviceKey="
            + encodedServiceKey()
            + "&stId="
            + station.stationId()
            + "&busRouteId="
            + route.busRouteId()
            + "&ord="
            + station.sequence();

    Document document = requestXml(requestUrl);
    logResponseSummary(document, "getArrInfoByRoute", route.busNo());
    List<TransitRouteResponse.RealtimeArrival> arrivals = new ArrayList<>();
    for (Element item : itemElements(document)) {
      addArrival(arrivals, route.busNo(), item, "1");
      addArrival(arrivals, route.busNo(), item, "2");
    }
    return arrivals;
  }

  private void addArrival(
      List<TransitRouteResponse.RealtimeArrival> arrivals,
      String busNo,
      Element item,
      String suffix) {
    String message = text(item, "arrmsg" + suffix);
    Integer seconds = intValue(text(item, "traTime" + suffix));
    if ((message == null || message.isBlank()) && seconds == null) {
      return;
    }

    Integer minutes = seconds == null ? null : Math.max(0, (int) Math.ceil(seconds / 60.0));
    arrivals.add(
        new TransitRouteResponse.RealtimeArrival(
            busNo,
            minutes,
            firstNonBlank(message, minutes == null ? null : minutes + "분 후 도착"),
            intValue(text(item, "staOrd" + suffix)),
            busTypeName(text(item, "busType" + suffix)),
            text(item, "stationNm" + suffix)));
  }

  private List<TransitRouteResponse.BusLocation> findBusLocations(
      RouteCandidate route, List<RouteStation> routeStations) throws Exception {
    String requestUrl =
        baseUrl
            + "/buspos/getBusPosByRtid"
            + "?serviceKey="
            + encodedServiceKey()
            + "&busRouteId="
            + route.busRouteId();

    Document document = requestXml(requestUrl);
    logResponseSummary(document, "getBusPosByRtid", route.busNo());
    List<TransitRouteResponse.BusLocation> locations = new ArrayList<>();
    for (Element item : itemElements(document)) {
      Double lng = doubleValue(firstNonBlank(text(item, "tmX"), text(item, "gpsX")));
      Double lat = doubleValue(firstNonBlank(text(item, "tmY"), text(item, "gpsY")));
      if (lng == null || lat == null) {
        continue;
      }
      String stationName = stationNameBySequence(routeStations, text(item, "sectOrd"));
      locations.add(new TransitRouteResponse.BusLocation(route.busNo(), lng, lat, stationName));
    }
    return locations;
  }

  private Document requestXml(String requestUrl) throws Exception {
    String rawResponse =
        webClient.get().uri(requestUrl).retrieve().bodyToMono(String.class).block();
    if (rawResponse == null || rawResponse.isBlank()) {
      throw new IllegalStateException("empty Seoul bus API response");
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(rawResponse)));
  }

  private List<Element> itemElements(Document document) {
    NodeList nodes = document.getElementsByTagName("itemList");
    List<Element> items = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element element) {
        items.add(element);
      }
    }
    return items;
  }

  private void logResponseSummary(Document document, String operation, String target) {
    String headerCode = firstText(document, "headerCd");
    String headerMessage = firstText(document, "headerMsg");
    int itemCount = document.getElementsByTagName("itemList").getLength();
    if (headerCode != null && !"0".equals(headerCode)) {
      log.warn(
          "Seoul bus API returned non-success. operation={}, target={}, headerCd={}, headerMsg={}",
          operation,
          target,
          headerCode,
          headerMessage);
      return;
    }
    log.debug(
        "Seoul bus API response. operation={}, target={}, itemCount={}",
        operation,
        target,
        itemCount);
  }

  private String firstText(Document document, String tagName) {
    NodeList nodes = document.getElementsByTagName(tagName);
    if (nodes.getLength() == 0 || nodes.item(0) == null) {
      return null;
    }
    String value = nodes.item(0).getTextContent();
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String stationNameBySequence(List<RouteStation> stations, String sequence) {
    if (sequence == null) {
      return null;
    }
    return stations.stream()
        .filter(station -> sequence.equals(station.sequence()))
        .map(RouteStation::stationName)
        .findFirst()
        .orElse(null);
  }

  private String text(Element element, String tagName) {
    NodeList nodes = element.getElementsByTagName(tagName);
    if (nodes.getLength() == 0 || nodes.item(0) == null) {
      return null;
    }
    String value = nodes.item(0).getTextContent();
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Integer intValue(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Double doubleValue(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private double squaredDistance(
      Double baseLat, Double baseLng, Double targetLat, Double targetLng) {
    double latDiff = baseLat - targetLat;
    double lngDiff = baseLng - targetLng;
    return latDiff * latDiff + lngDiff * lngDiff;
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

  private String busTypeName(String busType) {
    if (busType == null) {
      return null;
    }
    return switch (busType) {
      case "0" -> "일반버스";
      case "1" -> "저상버스";
      case "2" -> "굴절버스";
      default -> busType;
    };
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second == null || second.isBlank() ? null : second;
  }

  private String encodedServiceKey() {
    String key = serviceKey == null ? "" : serviceKey.trim();
    if (key.contains("%")) {
      key = URLDecoder.decode(key, StandardCharsets.UTF_8);
    }
    return URLEncoder.encode(key, StandardCharsets.UTF_8);
  }

  private record RouteCandidate(String busRouteId, String busNo) {}

  private record RouteStation(
      String stationId, String stationName, String sequence, Double lng, Double lat) {}
}
