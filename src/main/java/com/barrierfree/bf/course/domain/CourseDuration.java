package com.barrierfree.bf.course.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 여행 기간별 식사·관광·휴식 순서를 정의합니다. 2박 이상은 3일 기준으로 생성합니다. */
@Getter
@RequiredArgsConstructor
public enum CourseDuration {
  HALF_DAY("반나절", 1),
  FULL_DAY("하루", 1),
  ONE_NIGHT_TWO_DAYS("1박 2일", 2),
  TWO_NIGHTS_MORE("2박 3일 이상", 3);

  public enum CourseSlot {
    TOUR,
    FOOD,
    CAFE,
    LODGING
  }

  private final String label;
  private final int dayCount;

  public List<CourseSlot> getCompositionRule() {
    if (this == HALF_DAY) {
      return List.of(CourseSlot.TOUR, CourseSlot.FOOD, CourseSlot.CAFE);
    }
    List<CourseSlot> slots = new ArrayList<>();
    for (int day = 0; day < dayCount; day++) {
      slots.addAll(
          List.of(
              CourseSlot.TOUR, CourseSlot.FOOD, CourseSlot.TOUR, CourseSlot.CAFE, CourseSlot.FOOD));
      if (day < dayCount - 1) {
        slots.add(CourseSlot.LODGING);
      }
    }
    return List.copyOf(slots);
  }
}
