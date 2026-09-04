package com.barrierfree.bf.course.service;

import com.barrierfree.bf.course.domain.CourseRegion;
import com.barrierfree.bf.course.dto.AiCourseGenerateRequest;
import com.barrierfree.bf.course.dto.AiCoursePlacePreview;
import com.barrierfree.bf.course.dto.AiCoursePreviewResponse;
import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCourseGenerateService {

    private final PlaceService placeService;

    // 구글 맵 기반 지역 검색 시 사용할 반경 (미터 단위, 10km)
    private static final int SEARCH_RADIUS = 10000;

    /**
     * 유저의 입력(5단계)을 바탕으로 맞춤형 코스를 실시간으로 구성하여 반환합니다.
     */
    public AiCoursePreviewResponse generateCoursePreview(AiCourseGenerateRequest request) {
        log.info("AI 코스 생성 요청 수신: 지역={}, 테마={}, 일정={}",
            request.region().getLabel(), request.theme().getLabel(), request.duration().getLabel());

        List<String> compositionRule = request.duration().getCompositionRule();
        List<PlaceSearchResponse.PlaceSummary> selectedPlaces = new ArrayList<>();
        Set<String> usedPlaceIds = new HashSet<>();

        // 1. 코스 뼈대(Rule)에 맞춰 장소 순차 탐색 및 조합
        for (String rule : compositionRule) {
            PlaceSearchResponse.PlaceSummary selectedPlace = findBestPlaceForRule(rule, request, usedPlaceIds);

            if (selectedPlace != null) {
                selectedPlaces.add(selectedPlace);
                usedPlaceIds.add(selectedPlace.placeId()); // 중복 방지를 위한 기록
            } else {
                log.warn("조건에 맞는 '{}' 장소를 찾지 못했습니다. 일부 코스가 누락될 수 있습니다.", rule);
            }
        }

        // 2. 최소한의 장소도 찾지 못한 경우 예외 처리
        if (selectedPlaces.isEmpty()) {
            throw new CustomException(ErrorCode.PLACE_NOT_FOUND); // "조건에 맞는 장소를 찾을 수 없습니다." 에러코드 가정
        }

        // 3. 응답 DTO 맵핑
        List<AiCoursePlacePreview> placePreviews = new ArrayList<>();
        for (int i = 0; i < selectedPlaces.size(); i++) {
            placePreviews.add(AiCoursePlacePreview.from(i, selectedPlaces.get(i)));
        }

        // 4. 임시 코스 제목 생성 및 반환
        String courseTitle = generateTempTitle(request);

        return AiCoursePreviewResponse.of(courseTitle, request.duration().getDayCount(), placePreviews);
    }

    /**
     * 룰(명소, 식당, 숙박 등)에 맞춰 PlaceService를 호출하고 가장 적합한 장소를 1개 선택합니다.
     */
    private PlaceSearchResponse.PlaceSummary findBestPlaceForRule(
        String rule, AiCourseGenerateRequest request, Set<String> usedPlaceIds) {

        CourseRegion region = request.region();
        String keyword = region.getLabel() + " ";
        PlaceCategory targetCategory = PlaceCategory.ETC;

        // 룰에 따른 검색 키워드 및 카테고리 맵핑
        switch (rule) {
            case "명소/테마":
                keyword += request.theme().getLabel(); // 예: "제주 자연·휴식"
                targetCategory = request.theme().getTargetCategories().get(0);
                break;
            case "식당/카페":
                keyword += "맛집 카페"; // 예: "제주 맛집 카페"
                targetCategory = PlaceCategory.FOOD_CAFE;
                break;
            case "숙박":
                keyword += "장애인 객실 숙소 호텔";
                targetCategory = PlaceCategory.LODGING;
                break;
            default:
                keyword += "가볼만한곳";
        }

        try {
            // PlaceService.search() 호출 (기존에 구축된 접근성 필터링 로직이 내부에서 알아서 작동함)
            PlaceSearchResponse searchResponse = placeService.search(
                keyword,
                targetCategory.name(),
                region.getCenterLat(),
                region.getCenterLng(),
                SEARCH_RADIUS,
                10, // 충분한 후보군 확보를 위해 10개 요청
                null,
                request.mobilityTypes(), // 유저의 장애 타입 (필터링 핵심)
                List.of() // FacilityType은 별도로 받지 않으므로 빈 리스트
            );

            if (searchResponse == null || searchResponse.places().isEmpty()) {
                return null;
            }

            // 검색된 후보군 중, 이미 코스에 들어간 장소를 제외하고 첫 번째 장소 선택
            // (추후 Jina Embedding 텍스트 유사도 비교나 별점(Rating) 높은 순 정렬을 이 부분에 추가할 수 있습니다.)
            return searchResponse.places().stream()
                .filter(place -> !usedPlaceIds.contains(place.placeId()))
                .findFirst()
                .orElse(null);

        } catch (Exception e) {
            log.error("장소 검색 중 오류 발생. keyword: {}", keyword, e);
            return null;
        }
    }

    /**
     * 유저의 입력 데이터를 바탕으로 그럴듯한 코스 제목을 조합합니다.
     * (추후 LLM 연동 전까지 사용할 임시 로직)
     */
    private String generateTempTitle(AiCourseGenerateRequest request) {
        String regionName = request.region().getLabel();
        String companion = request.companion().getLabel();
        String theme = request.theme().getLabel();

        // 예: "제주, 가족과 여행 추천 - 자연·휴식 코스"
        return String.format("%s, %s 추천 - %s 코스", regionName, companion, theme);
    }
}
