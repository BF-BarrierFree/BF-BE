package com.barrierfree.bf.course.service;

import com.barrierfree.bf.course.domain.CourseDuration.CourseSlot;
import com.barrierfree.bf.course.domain.CourseTheme;
import com.barrierfree.bf.course.dto.AiCourseGenerateRequest;
import com.barrierfree.bf.course.dto.AiCoursePlacePreview;
import com.barrierfree.bf.course.dto.AiCoursePreviewResponse;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

  // 후보 선택은 가까운 반경부터 하지만, Google 후보 수집은 코스당 카테고리별 한 번만 합니다.
  private static final int[] SEARCH_RADII = {2000, 5000, 10000};
  private static final int COURSE_CANDIDATE_SEARCH_RADIUS = 10000;
  private static final int MAX_COURSE_CANDIDATE_SEARCHES = 5;

  public AiCoursePreviewResponse generateCoursePreview(AiCourseGenerateRequest request) {
    List<AiCoursePlacePreview> places = new ArrayList<>();
    Set<String> usedPlaceIds = new HashSet<>();
    SearchContext searchContext = new SearchContext();
    double lat = request.region().getCenterLat();
    double lng = request.region().getCenterLng();

    preloadCandidates(request, searchContext, lat, lng);

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
    PlaceCategory category = resolveCategory(slot, request);

    List<PlaceSearchResponse.PlaceSummary> cachedCandidates =
        List.copyOf(searchContext.candidates(category));

    // 접근성 정보가 확인된 후보를 먼저 찾습니다. Google에 접근성 데이터가 없는 지역에서도
    // 코스 자체가 사라지지 않도록, 확인된 후보가 없을 때만 일반 후보로 보완합니다.
    for (int radius : SEARCH_RADII) {
      PlaceSearchResponse.PlaceSummary cached =
          selectBestCandidate(
              cachedCandidates,
              category,
              usedPlaceIds,
              lat,
              lng,
              radius,
              request.mobilityTypes(),
              true);
      if (cached != null) {
        return cached;
      }
    }

    for (int radius : SEARCH_RADII) {
      PlaceSearchResponse.PlaceSummary cached =
          selectBestCandidate(
              cachedCandidates,
              category,
              usedPlaceIds,
              lat,
              lng,
              radius,
              request.mobilityTypes(),
              false);
      if (cached != null) {
        return cached;
      }
    }
    return null;
  }

  private void preloadCandidates(
      AiCourseGenerateRequest request, SearchContext searchContext, double lat, double lng) {
    Set<PlaceCategory> categories = new LinkedHashSet<>();
    for (CourseSlot slot : request.duration().getCompositionRule()) {
      categories.add(resolveCategory(slot, request));
    }

    for (PlaceCategory category : categories) {
      if (!searchContext.canSearch()) {
        log.warn("AI 코스 후보 수집 한도 도달: 최대호출={}", MAX_COURSE_CANDIDATE_SEARCHES);
        break;
      }
      searchContext.recordSearch();
      PlaceSearchResponse response =
          placeService.searchCourseCandidates(
              category, lat, lng, COURSE_CANDIDATE_SEARCH_RADIUS, request.mobilityTypes());
      searchContext.addCandidates(category, response.places());
    }
  }

  private PlaceCategory resolveCategory(CourseSlot slot, AiCourseGenerateRequest request) {
    return switch (slot) {
      case FOOD -> PlaceCategory.FOOD;
      case CAFE -> PlaceCategory.CAFE;
      case LODGING -> PlaceCategory.LODGING;
      case TOUR ->
          request.theme() == CourseTheme.FOOD_CAFE
              ? PlaceCategory.TOUR_CULTURE
              : request.theme().getTargetCategories().getFirst();
    };
  }

  private PlaceSearchResponse.PlaceSummary selectBestCandidate(
      List<PlaceSearchResponse.PlaceSummary> candidates,
      PlaceCategory category,
      Set<String> usedPlaceIds,
      double lat,
      double lng,
      int radius,
      List<MobilityType> mobilityTypes,
      boolean requireMobilityMatch) {
    return candidates.stream()
        .filter(place -> place.placeId() != null && !place.placeId().isBlank())
        .filter(place -> !usedPlaceIds.contains(place.placeId()))
        .filter(place -> place.category() == category)
        .filter(this::hasValidCoordinates)
        .filter(place -> distanceMeters(lat, lng, place) <= radius)
        .filter(place -> !requireMobilityMatch || matchesMobility(place, mobilityTypes))
        .min(Comparator.comparingDouble(place -> distanceMeters(lat, lng, place)))
        .orElse(null);
  }

  private boolean matchesMobility(
      PlaceSearchResponse.PlaceSummary place, List<MobilityType> mobilityTypes) {
    if (mobilityTypes == null || mobilityTypes.isEmpty()) {
      return true;
    }

    return mobilityTypes.stream()
        .filter(java.util.Objects::nonNull)
        .anyMatch(
            mobilityType ->
                switch (mobilityType) {
                  case WHEELCHAIR ->
                      Boolean.TRUE.equals(place.wheelchairAccessibleEntrance())
                          || Boolean.TRUE.equals(place.wheelchairAccessibleParking())
                          || Boolean.TRUE.equals(place.wheelchairAccessibleRestroom())
                          || Boolean.TRUE.equals(place.wheelchairAccessibleSeating())
                          || Boolean.TRUE.equals(place.ramp())
                          || Boolean.TRUE.equals(place.elevator());
                  case STROLLER ->
                      Boolean.TRUE.equals(place.strollerRental())
                          || Boolean.TRUE.equals(place.wheelchairAccessibleEntrance())
                          || Boolean.TRUE.equals(place.elevator());
                  case COGNITIVE_DEVELOPMENTAL ->
                      Boolean.TRUE.equals(place.restArea())
                          || Boolean.TRUE.equals(place.nursingRoom())
                          || Boolean.TRUE.equals(place.wheelchairAccessibleEntrance())
                          || Boolean.TRUE.equals(place.wheelchairAccessibleRestroom())
                          || Boolean.TRUE.equals(place.elevator());
                  case VISUAL_IMPAIRMENT ->
                      Boolean.TRUE.equals(place.voiceGuidance())
                          || Boolean.TRUE.equals(place.brailleBlock());
                  case HEARING_IMPAIRMENT ->
                      Boolean.TRUE.equals(place.signLanguage())
                          || Boolean.TRUE.equals(place.subtitleService())
                          || Boolean.TRUE.equals(place.hearingSupport());
                  case OTHER -> false;
                });
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
      return searchCount < MAX_COURSE_CANDIDATE_SEARCHES;
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
    String theme = request.theme().getLabel();

    // 미리보기 제목을 그대로 저장할 수 있도록 코스 이름의 15자 제한에 맞춥니다.
    return String.format("%s %s 코스", regionName, theme);
  }
}
