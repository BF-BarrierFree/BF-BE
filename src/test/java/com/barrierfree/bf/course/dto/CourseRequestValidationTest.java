package com.barrierfree.bf.course.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.util.Collections;
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
}
