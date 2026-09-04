package com.barrierfree.bf.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.service.PlaceService;
import java.util.List;
import org.junit.jupiter.api.Test;
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
    verify(placeService, times(2))
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

  private AiCourseGenerateRequest generateRequest() {
    return new AiCourseGenerateRequest(
        CourseRegion.SEOUL,
        CourseCompanion.ALONE,
        List.of(),
        CourseTheme.NATURE_HEALING,
        CourseDuration.HALF_DAY);
  }
}
