package com.barrierfree.bf.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.course.domain.CourseCompanion;
import com.barrierfree.bf.course.domain.CourseDuration;
import com.barrierfree.bf.course.domain.CourseRegion;
import com.barrierfree.bf.course.domain.CourseTheme;
import com.barrierfree.bf.course.dto.AiCourseGenerateRequest;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiCourseGenerateServiceTest {

  private final PlaceService placeService = Mockito.mock(PlaceService.class);
  private final AiCourseGenerateService service = new AiCourseGenerateService(placeService);

  @Test
  void propagatesCourseCandidateSearchFailures() {
    when(placeService.searchCourseCandidates(
            any(PlaceCategory.class), anyDouble(), anyDouble(), anyInt(), anyList()))
        .thenThrow(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));

    assertThatThrownBy(() -> service.generateCoursePreview(generateRequest()))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_MAP_API_FAILED));
  }

  @Test
  void loadsCandidatesOncePerDistinctCategoryAndReusesThemForTheWholeCourse() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.searchCourseCandidates(
            any(PlaceCategory.class), anyDouble(), anyDouble(), anyInt(), anyList()))
        .thenAnswer(
            invocation -> {
              PlaceCategory category = invocation.getArgument(0);
              return new PlaceSearchResponse(
                  List.of(
                      place(category + "-1", category, lat, lng),
                      place(category + "-2", category, lat + .001, lng)),
                  null,
                  false);
            });

    var preview =
        service.generateCoursePreview(
            new AiCourseGenerateRequest(
                CourseRegion.SEOUL,
                CourseCompanion.ALONE,
                List.of(),
                CourseTheme.NATURE_HEALING,
                CourseDuration.FULL_DAY));

    assertThat(preview.places()).isNotEmpty();
    verify(placeService, times(3))
        .searchCourseCandidates(
            any(PlaceCategory.class), anyDouble(), anyDouble(), anyInt(), anyList());
  }

  @Test
  void failsWhenARequiredCourseCategoryHasNoCandidates() {
    when(placeService.searchCourseCandidates(
            any(PlaceCategory.class), anyDouble(), anyDouble(), anyInt(), anyList()))
        .thenReturn(new PlaceSearchResponse(List.of(), null, false));

    assertThatThrownBy(() -> service.generateCoursePreview(generateRequest()))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLACE_NOT_FOUND));
  }

  @Test
  void keepsCourseCandidatesWhenGoogleHasNoAccessibilityData() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.searchCourseCandidates(
            any(PlaceCategory.class), anyDouble(), anyDouble(), anyInt(), anyList()))
        .thenAnswer(
            invocation -> {
              PlaceCategory category = invocation.getArgument(0);
              return new PlaceSearchResponse(
                  List.of(
                      place(category + "-1", category, lat, lng),
                      place(category + "-2", category, lat + .001, lng)),
                  null,
                  false);
            });

    var preview =
        service.generateCoursePreview(
            new AiCourseGenerateRequest(
                CourseRegion.SEOUL,
                CourseCompanion.ALONE,
                List.of(MobilityType.WHEELCHAIR),
                CourseTheme.FOOD_CAFE,
                CourseDuration.HALF_DAY));

    assertThat(preview.places()).hasSize(3);
  }

  private PlaceSearchResponse.PlaceSummary place(
      String id, PlaceCategory category, Double lat, Double lng) {
    return new PlaceSearchResponse.PlaceSummary(
        id,
        id,
        null,
        lat,
        lng,
        category,
        category.getLabel(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private AiCourseGenerateRequest generateRequest() {
    return new AiCourseGenerateRequest(
        CourseRegion.SEOUL,
        CourseCompanion.ALONE,
        List.of(),
        CourseTheme.NATURE_HEALING,
        CourseDuration.HALF_DAY);
  }
}
