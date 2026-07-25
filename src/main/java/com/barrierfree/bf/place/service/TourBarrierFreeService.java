package com.barrierfree.bf.place.service;

import com.barrierfree.bf.place.dto.PublicBarrierFreeInfo;
import com.barrierfree.bf.place.dto.PublicBarrierFreePlace;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourBarrierFreeService {

  private static final String MOBILE_OS = "ETC";
  private static final String MOBILE_APP = "BarrierFree";

  @Value("${public-data.tour-api.base-url:https://apis.data.go.kr/B551011/KorWithService2}")
  private String baseUrl;

  @Value("${public-data.tour-api.service-key:${DATA_GO_KR_API_KEY}}")
  private String serviceKey;

  private final WebClient webClient;

  public PublicBarrierFreeInfo findByPlaceName(String placeName) {
    if (placeName == null || placeName.isBlank()) {
      return PublicBarrierFreeInfo.empty();
    }

    try {
      String contentId = findContentId(placeName);
      if (contentId == null) {
        return PublicBarrierFreeInfo.notFound();
      }

      JsonNode item = fetchBarrierFreeDetail(contentId);
      if (item == null || item.isMissingNode()) {
        return PublicBarrierFreeInfo.notFound();
      }

      return toBarrierFreeInfo(item);
    } catch (RuntimeException e) {
      log.warn("TourAPI barrier-free lookup failed. placeName={}", placeName, e);
      return PublicBarrierFreeInfo.apiFailed();
    }
  }

  public List<PublicBarrierFreePlace> searchByKeyword(String keyword, int limit) {
    if (keyword == null || keyword.isBlank() || limit < 1) {
      return List.of();
    }

    try {
      JsonNode response = fetchKeywordSearch(keyword, Math.max(limit * 3, 10));
      JsonNode items = response == null ? null : response.at("/response/body/items/item");
      if (items == null || items.isMissingNode() || items.isNull()) {
        return List.of();
      }

      List<PublicBarrierFreePlace> places = new ArrayList<>();
      List<JsonNode> candidates = new ArrayList<>();
      if (items.isArray()) {
        items.forEach(candidates::add);
      } else {
        candidates.add(items);
      }

      int parallelism = Math.max(1, Math.min(4, limit));
      ExecutorService executor = Executors.newFixedThreadPool(parallelism);
      CompletionService<PublicBarrierFreePlace> completionService =
          new java.util.concurrent.ExecutorCompletionService<>(executor);

      List<Future<PublicBarrierFreePlace>> submittedTasks = new ArrayList<>();
      int inFlight = 0;
      int candidateIndex = 0;
      try {
        while (inFlight < parallelism && candidateIndex < candidates.size()) {
          JsonNode candidate = candidates.get(candidateIndex++);
          submittedTasks.add(completionService.submit(() -> addPublicPlaceIfAvailable(candidate)));
          inFlight++;
        }

        while (inFlight > 0 && places.size() < limit) {
          try {
            Future<PublicBarrierFreePlace> completed = completionService.take();
            inFlight--;

            PublicBarrierFreePlace place = completed.get();
            if (place != null) {
              places.add(place);
              if (places.size() >= limit) {
                break;
              }
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          } catch (ExecutionException e) {
            log.debug("TourAPI barrier-free candidate lookup failed", e.getCause());
          }

          while (inFlight < parallelism && candidateIndex < candidates.size() && places.size() < limit) {
            JsonNode candidate = candidates.get(candidateIndex++);
            submittedTasks.add(completionService.submit(() -> addPublicPlaceIfAvailable(candidate)));
            inFlight++;
          }
        }
      } finally {
        if (places.size() >= limit) {
          submittedTasks.forEach(future -> future.cancel(true));
        }
        executor.shutdownNow();
      }

      return places;
    } catch (RuntimeException e) {
      log.warn("TourAPI barrier-free keyword search failed. keyword={}", keyword, e);
      return List.of();
    }
  }

  public PublicBarrierFreePlace findByContentId(String contentId) {
    if (contentId == null || contentId.isBlank()) {
      return null;
    }

    try {
      JsonNode commonDetail = fetchCommonDetail(contentId);
      JsonNode commonItem = firstItem(commonDetail);
      JsonNode barrierFreeDetail = fetchBarrierFreeDetail(contentId);
      JsonNode barrierItem = barrierFreeDetail == null || barrierFreeDetail.isMissingNode()
          ? null
          : barrierFreeDetail;

      if (commonItem == null && barrierItem == null) {
        return null;
      }

      PublicBarrierFreeInfo barrierFreeInfo =
          barrierItem == null ? PublicBarrierFreeInfo.notFound() : toBarrierFreeInfo(barrierItem);

      return new PublicBarrierFreePlace(
          contentId,
          commonItem == null ? contentId : text(commonItem, "title"),
          commonItem == null ? null : address(commonItem),
          commonItem == null ? null : doubleValue(commonItem, "mapy"),
          commonItem == null ? null : doubleValue(commonItem, "mapx"),
          barrierFreeInfo);
    } catch (RuntimeException e) {
      log.warn("TourAPI barrier-free detail lookup failed. contentId={}", contentId, e);
      return null;
    }
  }

  private String findContentId(String placeName) {
    JsonNode response = fetchKeywordSearch(placeName, 10);
    JsonNode item = bestMatchedItem(response, placeName);
    return item == null ? null : text(item, "contentid");
  }

  private JsonNode fetchKeywordSearch(String keyword, int numOfRows) {
    return webClient
        .get()
        .uri(
            UriComponentsBuilder.fromUriString(baseUrl + "/searchKeyword2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", 1)
                .queryParam("keyword", keyword)
                .build()
                .encode()
                .toUri())
        .retrieve()
        .bodyToMono(JsonNode.class)
        .block();
  }

  private JsonNode fetchBarrierFreeDetail(String contentId) {
    JsonNode response =
        webClient
            .get()
            .uri(
                UriComponentsBuilder.fromUriString(baseUrl + "/detailWithTour2")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("MobileOS", MOBILE_OS)
                    .queryParam("MobileApp", MOBILE_APP)
                    .queryParam("_type", "json")
                    .queryParam("contentId", contentId)
                    .build()
                    .encode()
                    .toUri())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    return firstItem(response);
  }

  private JsonNode fetchCommonDetail(String contentId) {
    return webClient
        .get()
        .uri(
            UriComponentsBuilder.fromUriString(baseUrl + "/detailCommon2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", MOBILE_OS)
                .queryParam("MobileApp", MOBILE_APP)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .build()
                .encode()
                .toUri())
        .retrieve()
        .bodyToMono(JsonNode.class)
        .block();
  }

  private PublicBarrierFreePlace addPublicPlaceIfAvailable(JsonNode item) {
    String contentId = text(item, "contentid");
    if (contentId == null || contentId.isBlank()) {
      return null;
    }

    JsonNode detail = fetchBarrierFreeDetail(contentId);
    if (detail == null || detail.isMissingNode()) {
      return null;
    }

    PublicBarrierFreeInfo barrierFreeInfo = toBarrierFreeInfo(detail);
    if (!barrierFreeInfo.hasAnyValue()) {
      return null;
    }

    return new PublicBarrierFreePlace(
        contentId,
        text(item, "title"),
        address(item),
        doubleValue(item, "mapy"),
        doubleValue(item, "mapx"),
        barrierFreeInfo);
  }

  private PublicBarrierFreeInfo toBarrierFreeInfo(JsonNode item) {
    return new PublicBarrierFreeInfo(
        hasPositiveInfo(item, "elevator"),
        hasPositiveInfo(item, "exit", "route", "publictransport"),
        hasPositiveInfo(item, "parking"),
        hasPositiveInfo(item, "auditorium"),
        hasPositiveInfo(item, "restroom"),
        hasPositiveInfo(item, "audioguide"),
        hasPositiveInfo(item, "braileblock", "brailepromotion", "guidesystem"),
        hasPositiveInfo(item, "signguide", "videoguide", "hearinghandicapetc"),
        hasPositiveInfo(item, "stroller"),
        hasPositiveInfo(item, "lactationroom"),
        hasPositiveInfo(item, "wheelchair"),
        hasPositiveInfo(item, "signguide"),
        hasPositiveInfo(item, "handicapetc", "infantsfamilyetc"),
        hasPositiveInfo(item, "videoguide", "hearinghandicapetc"),
        "GOOGLE_PLACES_AND_PUBLIC_DATA");
  }

  private JsonNode firstItem(JsonNode response) {
    JsonNode items = response == null ? null : response.at("/response/body/items/item");
    if (items == null || items.isMissingNode() || items.isNull()) {
      return null;
    }
    return items.isArray() ? (items.isEmpty() ? null : items.get(0)) : items;
  }

  private JsonNode bestMatchedItem(JsonNode response, String placeName) {
    JsonNode items = response == null ? null : response.at("/response/body/items/item");
    if (items == null || items.isMissingNode() || items.isNull()) {
      return null;
    }
    if (!items.isArray()) {
      return items;
    }

    String normalizedPlaceName = normalize(placeName);
    JsonNode first = items.isEmpty() ? null : items.get(0);
    for (JsonNode item : items) {
      String title = normalize(text(item, "title"));
      if (!title.isBlank()
          && (title.equals(normalizedPlaceName)
              || title.contains(normalizedPlaceName)
              || normalizedPlaceName.contains(title))) {
        return item;
      }
    }
    return first;
  }

  private Boolean hasPositiveInfo(JsonNode item, String... fields) {
    return List.of(fields).stream()
        .map(field -> text(item, field))
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .map(this::isPositiveText)
        .orElse(null);
  }

  private String text(JsonNode node, String fieldName) {
    JsonNode value = node.get(fieldName);
    return value == null || value.isNull() ? null : value.asText();
  }

  private String address(JsonNode item) {
    String addr1 = text(item, "addr1");
    String addr2 = text(item, "addr2");
    if (addr2 == null || addr2.isBlank()) {
      return addr1;
    }
    return addr1 == null || addr1.isBlank() ? addr2 : addr1 + " " + addr2;
  }

  private Double doubleValue(JsonNode item, String fieldName) {
    String value = text(item, fieldName);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Double.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private boolean isPositiveText(String value) {
    String normalized = value.replaceAll("\\s+", "");
    return !normalized.isBlank()
        && !normalized.contains("없음")
        && !normalized.contains("없슴")
        && !normalized.contains("불가")
        && !normalized.contains("미제공")
        && !normalized.contains("해당없음")
        && !normalized.equals("0");
  }

  private String normalize(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
  }
}
