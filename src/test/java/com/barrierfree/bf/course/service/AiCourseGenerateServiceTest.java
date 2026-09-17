package com.barrierfree.bf.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.course.domain.CourseCompanion;
import com.barrierfree.bf.course.domain.CourseDuration;
import com.barrierfree.bf.course.domain.CourseRegion;
import com.barrierfree.bf.course.domain.CourseTheme;
import com.barrierfree.bf.course.dto.AiCourseGenerateRequest;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

class AiCourseGenerateServiceTest {

  private final PlaceService placeService = Mockito.mock(PlaceService.class);
  private final AiCourseGenerateService service = new AiCourseGenerateService(placeService);

  @Test
  void propagatesPlaceSearchFailures() {
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenThrow(new CustomException(ErrorCode.GOOGLE_MAP_API_FAILED));

    assertThatThrownBy(() -> service.generateCoursePreview(generateRequest()))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_MAP_API_FAILED));
  }

  @Test
  void reportsPlaceNotFoundWhenSearchResultsAreEmpty() {
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenReturn(new PlaceSearchResponse(List.of(), null, false));

    assertThatThrownBy(() -> service.generateCoursePreview(generateRequest()))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLACE_NOT_FOUND));
    verify(placeService, times(6))
        .search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList());
  }

  @ParameterizedTest
  @EnumSource(CourseDuration.class)
  void balancesEveryDurationEvenForFoodCafeTheme(CourseDuration duration) {
    AtomicInteger ids = new AtomicInteger();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenAnswer(
            invocation ->
                new PlaceSearchResponse(
                    List.of(
                        place(
                            "p" + ids.incrementAndGet(),
                            PlaceCategory.valueOf(invocation.getArgument(1)),
                            invocation.getArgument(2),
                            invocation.getArgument(3))),
                    null,
                    false));

    var result =
        service.generateCoursePreview(
            new AiCourseGenerateRequest(
                CourseRegion.SEOUL,
                CourseCompanion.ALONE,
                List.of(),
                CourseTheme.FOOD_CAFE,
                duration));

    int days = duration.getDayCount();
    boolean halfDay = duration == CourseDuration.HALF_DAY;
    assertThat(result.totalDays()).isEqualTo(days);
    assertThat(result.places().stream().filter(p -> p.category() == PlaceCategory.FOOD))
        .hasSize(halfDay ? 1 : 2 * days);
    assertThat(result.places().stream().filter(p -> p.category() == PlaceCategory.TOUR_CULTURE))
        .hasSize(halfDay ? 1 : 2 * days);
    assertThat(result.places().stream().filter(p -> p.category() == PlaceCategory.CAFE))
        .hasSize(days);
    assertThat(result.places().stream().filter(p -> p.category() == PlaceCategory.LODGING))
        .hasSize(days - 1);
    assertThat(result.places()).extracting(p -> p.placeId()).doesNotHaveDuplicates();
    assertThat(result.places())
        .extracting(p -> p.sequence())
        .containsExactlyElementsOf(
            java.util.stream.IntStream.range(0, result.places().size()).boxed().toList());
    var expected = new java.util.ArrayList<PlaceCategory>();
    for (int day = 0; day < days; day++) {
      expected.addAll(
          halfDay
              ? List.of(PlaceCategory.TOUR_CULTURE, PlaceCategory.FOOD, PlaceCategory.CAFE)
              : List.of(
                  PlaceCategory.TOUR_CULTURE,
                  PlaceCategory.FOOD,
                  PlaceCategory.TOUR_CULTURE,
                  PlaceCategory.CAFE,
                  PlaceCategory.FOOD));
      if (day < days - 1) expected.add(PlaceCategory.LODGING);
    }
    assertThat(result.places()).extracting(p -> p.category()).containsExactlyElementsOf(expected);
  }

  @Test
  void everyGeneratedTitleFitsSaveRequestLimit() {
    // given
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenAnswer(
            invocation ->
                new PlaceSearchResponse(
                    List.of(
                        place(
                            invocation.getArgument(1),
                            PlaceCategory.valueOf(invocation.getArgument(1)),
                            invocation.getArgument(2),
                            invocation.getArgument(3))),
                    null,
                    false));

    // when / then
    for (CourseRegion region : CourseRegion.values()) {
      for (CourseCompanion companion : CourseCompanion.values()) {
        for (CourseTheme theme : CourseTheme.values()) {
          var preview =
              service.generateCoursePreview(
                  new AiCourseGenerateRequest(
                      region, companion, List.of(), theme, CourseDuration.HALF_DAY));
          assertThat(preview.courseTitle()).isNotBlank().hasSizeLessThanOrEqualTo(15);
        }
      }
    }
  }

  @Test
  void choosesNearestValidPlaceAndSearchesFromPreviousStop() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenReturn(
            new PlaceSearchResponse(
                List.of(
                    place("far", PlaceCategory.PARK_TRAIL, lat + .01, lng),
                    place("invalid", PlaceCategory.PARK_TRAIL, null, lng),
                    place("wrong", PlaceCategory.CAFE, lat, lng),
                    place("tour", PlaceCategory.PARK_TRAIL, lat + .001, lng)),
                null,
                false),
            new PlaceSearchResponse(
                List.of(
                    place("tour", PlaceCategory.FOOD, lat + .001, lng),
                    place("food", PlaceCategory.FOOD, lat + .002, lng)),
                null,
                false),
            new PlaceSearchResponse(
                List.of(place("cafe", PlaceCategory.CAFE, lat + .003, lng)), null, false));

    var result = service.generateCoursePreview(generateRequest());
    assertThat(result.places())
        .extracting(p -> p.placeId())
        .containsExactly("tour", "food", "cafe");
    verify(placeService)
        .search(
            anyString(),
            eq("FOOD"),
            eq(lat + .001),
            eq(lng),
            eq(2000),
            eq(20),
            isNull(),
            anyList(),
            anyList());
    verify(placeService)
        .search(
            anyString(),
            eq("CAFE"),
            eq(lat + .002),
            eq(lng),
            eq(2000),
            eq(20),
            isNull(),
            anyList(),
            anyList());
  }

  @Test
  void expandsRadiusWhenNearbyCandidatesAreOutsideBoundary() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenAnswer(
            invocation -> {
              PlaceCategory category = PlaceCategory.valueOf(invocation.getArgument(1));
              return new PlaceSearchResponse(
                  List.of(place(category.name(), category, lat + .03, lng)), null, false);
            });
    assertThat(service.generateCoursePreview(generateRequest()).places()).hasSize(3);
    verify(placeService)
        .search(
            anyString(),
            eq("PARK_TRAIL"),
            eq(lat),
            eq(lng),
            eq(5000),
            eq(20),
            isNull(),
            anyList(),
            anyList());
  }

  @Test
  void rejectsIncompleteCourseWhenRequiredFoodIsMissing() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenReturn(
            new PlaceSearchResponse(
                List.of(place("tour", PlaceCategory.PARK_TRAIL, lat, lng)), null, false),
            new PlaceSearchResponse(
                List.of(place("cafe", PlaceCategory.CAFE, lat, lng)), null, false));
    assertThatThrownBy(() -> service.generateCoursePreview(generateRequest()))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_NOT_FOUND));
  }

  @Test
  void skipsOptionalSlotsWhenNoMatchingPlaceExists() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenAnswer(
            invocation -> {
              PlaceCategory category = PlaceCategory.valueOf(invocation.getArgument(1));
              if (category == PlaceCategory.FOOD) {
                return new PlaceSearchResponse(
                    List.of(place("food", category, lat, lng)), null, false);
              }
              return new PlaceSearchResponse(List.of(), null, false);
            });

    var result = service.generateCoursePreview(generateRequest());

    assertThat(result.places()).extracting(p -> p.placeId()).containsExactly("food");
    assertThat(result.places()).extracting(p -> p.sequence()).containsExactly(0);
  }

  @Test
  void reusesCandidatesForRepeatedCategoriesWithinOneRequest() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenAnswer(
            invocation -> {
              PlaceCategory category = PlaceCategory.valueOf(invocation.getArgument(1));
              return new PlaceSearchResponse(
                  List.of(
                      place(category + "-1", category, lat, lng),
                      place(category + "-2", category, lat + .001, lng)),
                  null,
                  false);
            });

    var result =
        service.generateCoursePreview(
            new AiCourseGenerateRequest(
                CourseRegion.SEOUL,
                CourseCompanion.ALONE,
                List.of(),
                CourseTheme.NATURE_HEALING,
                CourseDuration.FULL_DAY));

    assertThat(result.places()).hasSize(5);
    verify(placeService, times(3))
        .search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList());
  }

  @Test
  void boundsSearchCallsWhileKeepingRequiredCachedPlaces() {
    double lat = CourseRegion.SEOUL.getCenterLat();
    double lng = CourseRegion.SEOUL.getCenterLng();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenAnswer(
            invocation -> {
              PlaceCategory category = PlaceCategory.valueOf(invocation.getArgument(1));
              if (category != PlaceCategory.FOOD && category != PlaceCategory.LODGING) {
                return new PlaceSearchResponse(List.of(), null, false);
              }
              return new PlaceSearchResponse(
                  java.util.stream.IntStream.range(0, 20)
                      .mapToObj(index -> place(category + "-" + index, category, lat, lng))
                      .toList(),
                  null,
                  false);
            });

    var result =
        service.generateCoursePreview(
            new AiCourseGenerateRequest(
                CourseRegion.SEOUL,
                CourseCompanion.ALONE,
                List.of(),
                CourseTheme.NATURE_HEALING,
                CourseDuration.TWO_NIGHTS_MORE));

    assertThat(result.places())
        .allMatch(
            place ->
                place.category() == PlaceCategory.FOOD
                    || place.category() == PlaceCategory.LODGING);
    verify(placeService, times(24))
        .search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList());
  }

  private PlaceSearchResponse.PlaceSummary place(
      String id, PlaceCategory category, Double lat, Double lng) {
    return new PlaceSearchResponse.PlaceSummary(
        id, null, null, lat, lng, category, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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
