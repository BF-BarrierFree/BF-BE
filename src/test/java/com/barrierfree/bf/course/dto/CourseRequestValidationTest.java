package com.barrierfree.bf.course.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.barrierfree.bf.place.domain.PlaceCategory;
import jakarta.validation.Validation;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseRequestValidationTest {

  @Test
  void rejectsNullSavedPlaceIds() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();

      var createViolations =
          validator.validate(new CourseCreateRequest("서울 코스", Collections.singletonList(null)));
      var updateViolations =
          validator.validate(new CourseUpdateRequest("서울 코스", Collections.singletonList(null)));

      assertThat(createViolations).hasSize(1);
      assertThat(updateViolations).hasSize(1);
    }
  }

  @Test
  void validatesRequiredAiCoursePlaceFields() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();
      var place = new AiCoursePlaceSaveDto(null, null, null, null, null, null, null);

      var violations = validator.validate(new AiCourseSaveRequest("서울 코스", List.of(place)));

      assertThat(violations)
          .extracting(violation -> violation.getPropertyPath().toString())
          .containsExactlyInAnyOrder(
              "places[0].placeId", "places[0].name", "places[0].latitude", "places[0].longitude");
    }
  }

  @Test
  void rejectsNullAiCoursePlaceElements() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();

      var violations =
          validator.validate(new AiCourseSaveRequest("서울 코스", Collections.singletonList(null)));

      assertThat(violations).hasSize(1);
    }
  }

  @Test
  void rejectsMoreThanSevenAiCoursePlaces() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();
      var place =
          new AiCoursePlaceSaveDto(
              "place-1", "서울역", PlaceCategory.TRANSPORTATION, null, 37.55, 126.97, null);

      var violations =
          validator.validate(new AiCourseSaveRequest("서울 코스", Collections.nCopies(8, place)));

      assertThat(violations).hasSize(1);
    }
  }
}
