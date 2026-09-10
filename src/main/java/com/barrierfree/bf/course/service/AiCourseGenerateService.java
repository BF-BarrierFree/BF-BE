package com.barrierfree.bf.course.service;

import com.barrierfree.bf.course.domain.CourseDuration.CourseSlot;
import com.barrierfree.bf.course.domain.CourseTheme;
import com.barrierfree.bf.course.dto.AiCourseGenerateRequest;
import com.barrierfree.bf.course.dto.AiCoursePlacePreview;
import com.barrierfree.bf.course.dto.AiCoursePreviewResponse;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCourseGenerateService {

  private final PlaceService placeService;

  // 가까운 후보부터 검색하고, 부족할 때만 최대 10km까지 확장합니다.
  private static final int[] SEARCH_RADII = {2000, 5000, 10000};
  // 가장 긴 일정의 17개 슬롯을 모두 검색할 수 있게 하되, 반경 확장 재시도는 제한합니다.
  private static final int MAX_PLACE_SEARCHES_PER_REQUEST = 24;

  public AiCoursePreviewResponse generateCoursePreview(AiCourseGenerateRequest request) {
    List<AiCoursePlacePreview> places = new ArrayList<>();
    Set<String> usedPlaceIds = new HashSet<>();
    SearchContext searchContext = new SearchContext();
    double lat = request.region().getCenterLat();
    double lng = request.region().getCenterLng();

    for (CourseSlot slot : request.duration().getCompositionRule()) {
      PlaceSearchResponse.PlaceSummary selected =
          findBestPlaceForSlot(slot, request, usedPlaceIds, lat, lng, searchContext);
      if (selected == null) {
        if (isRequired(slot)) {
          log.warn("AI 코스 필수 장소 검색 실패: 지역={}, 슬롯={}", request.region(), slot);
          throw new CustomException(ErrorCode.PLACE_NOT_FOUND);
        }
        log.warn("AI 코스 선택 장소 검색 실패로 슬롯을 건너뜁니다: 지역={}, 슬롯={}", request.region(), slot);
        continue;
      }
      places.add(AiCoursePlacePreview.from(places.size(), selected));
      usedPlaceIds.add(selected.placeId());
      lat = selected.lat();
      lng = selected.lng();
    }
    return AiCoursePreviewResponse.of(
        generateTempTitle(request), request.duration().getDayCount(), places);
  }

  private PlaceSearchResponse.PlaceSummary findBestPlaceForSlot(
      CourseSlot slot,
      AiCourseGenerateRequest request,
      Set<String> usedPlaceIds,
      double lat,
      double lng,
      SearchContext searchContext) {
    PlaceCategory category =
        switch (slot) {
          case FOOD -> PlaceCategory.FOOD;
          case CAFE -> PlaceCategory.CAFE;
          case LODGING -> PlaceCategory.LODGING;
          case TOUR ->
              request.theme() == CourseTheme.FOOD_CAFE
                  ? PlaceCategory.TOUR_CULTURE
                  : request.theme().getTargetCategories().getFirst();
        };
    String keyword =
        request.region().getLabel()
            + " "
            + switch (slot) {
              case FOOD -> "맛집 음식점";
              case CAFE -> "카페";
              case LODGING -> "장애인 객실 숙소 호텔";
              case TOUR ->
                  request.theme() == CourseTheme.FOOD_CAFE ? "관광 명소" : request.theme().getLabel();
            };

    List<PlaceSearchResponse.PlaceSummary> cachedCandidates =
        List.copyOf(searchContext.candidates(category));

    for (int radius : SEARCH_RADII) {
      PlaceSearchResponse.PlaceSummary cached =
          selectBestCandidate(cachedCandidates, category, usedPlaceIds, lat, lng, radius);
      if (cached != null) {
        return cached;
      }
      if (!searchContext.canSearch()) {
        log.warn("AI 코스 장소 검색 한도 도달: 최대호출={}", MAX_PLACE_SEARCHES_PER_REQUEST);
        return null;
      }
      searchContext.recordSearch();
      PlaceSearchResponse response =
          placeService.search(
              keyword,
              category.name(),
              lat,
              lng,
              radius,
              20,
              null,
              request.mobilityTypes(),
              List.of());
      searchContext.addCandidates(category, response.places());
      PlaceSearchResponse.PlaceSummary selected =
          selectBestCandidate(
              searchContext.candidates(category), category, usedPlaceIds, lat, lng, radius);
      if (selected != null) {
        return selected;
      }
    }
    return null;
  }

  private PlaceSearchResponse.PlaceSummary selectBestCandidate(
      List<PlaceSearchResponse.PlaceSummary> candidates,
      PlaceCategory category,
      Set<String> usedPlaceIds,
      double lat,
      double lng,
      int radius) {
    return candidates.stream()
        .filter(place -> place.placeId() != null && !place.placeId().isBlank())
        .filter(place -> !usedPlaceIds.contains(place.placeId()))
        .filter(place -> place.category() == category)
        .filter(this::hasValidCoordinates)
        .filter(place -> distanceMeters(lat, lng, place) <= radius)
        .min(Comparator.comparingDouble(place -> distanceMeters(lat, lng, place)))
        .orElse(null);
  }

  private boolean isRequired(CourseSlot slot) {
    return slot == CourseSlot.FOOD || slot == CourseSlot.LODGING;
  }

  private static final class SearchContext {
    private final Map<PlaceCategory, List<PlaceSearchResponse.PlaceSummary>> candidates =
        new EnumMap<>(PlaceCategory.class);
    private int searchCount;

    private List<PlaceSearchResponse.PlaceSummary> candidates(PlaceCategory category) {
      return candidates.computeIfAbsent(category, ignored -> new ArrayList<>());
    }

    private void addCandidates(
        PlaceCategory category, List<PlaceSearchResponse.PlaceSummary> newCandidates) {
      candidates(category).addAll(newCandidates);
    }

    private boolean canSearch() {
      return searchCount < MAX_PLACE_SEARCHES_PER_REQUEST;
    }

    private void recordSearch() {
      searchCount++;
    }
  }

  private boolean hasValidCoordinates(PlaceSearchResponse.PlaceSummary place) {
    return place.lat() != null
        && place.lng() != null
        && Double.isFinite(place.lat())
        && Double.isFinite(place.lng())
        && Math.abs(place.lat()) <= 90
        && Math.abs(place.lng()) <= 180;
  }

  /** 직선 거리 기준이며 실제 보행 경로나 이동 시간을 의미하지 않습니다. */
  private double distanceMeters(double lat, double lng, PlaceSearchResponse.PlaceSummary place) {
    double dLat = Math.toRadians(place.lat() - lat);
    double dLng = Math.toRadians(place.lng() - lng);
    double a =
        Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(lat))
                * Math.cos(Math.toRadians(place.lat()))
                * Math.pow(Math.sin(dLng / 2), 2);
    return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
  }

  /** 유저의 입력 데이터를 바탕으로 그럴듯한 코스 제목을 조합합니다. (추후 LLM 연동 전까지 사용할 임시 로직) */
  private String generateTempTitle(AiCourseGenerateRequest request) {
    String regionName = request.region().getLabel();
    String companion = request.companion().getLabel();
    String theme = request.theme().getLabel();

    // 예: "제주, 가족과 여행 추천 - 자연·휴식 코스"
    return String.format("%s, %s 추천 - %s 코스", regionName, companion, theme);
  }
}
