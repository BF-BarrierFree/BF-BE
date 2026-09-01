package com.barrierfree.bf.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.course.dto.CourseCreateRequest;
import com.barrierfree.bf.course.entity.Course;
import com.barrierfree.bf.course.repository.CourseRepository;
import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.entity.SavedPlace;
import com.barrierfree.bf.place.entity.SavedPlaceList;
import com.barrierfree.bf.place.repository.SavedPlaceRepository;
import com.barrierfree.bf.route.service.OrsRouteService;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CourseServiceTest {

  private final CourseRepository courseRepository = Mockito.mock(CourseRepository.class);
  private final SavedPlaceRepository savedPlaceRepository =
      Mockito.mock(SavedPlaceRepository.class);
  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final OrsRouteService orsRouteService = Mockito.mock(OrsRouteService.class);
  private final CourseService service =
      new CourseService(courseRepository, savedPlaceRepository, userRepository, orsRouteService);

  @Test
  void rejectsSavedPlaceNotOwnedByRequestingUser() {
    User requestingUser =
        User.builder().socialId("kakao-1").nickname("requester").role(Role.USER).build();
    User otherUser = User.builder().socialId("kakao-2").nickname("other").role(Role.USER).build();
    SavedPlace foreignPlace =
        new SavedPlace(
            new SavedPlaceList(otherUser, "다른 사용자의 목록"),
            "place-1",
            "서울역",
            PlaceCategory.TRANSPORTATION,
            true,
            "서울 중구",
            37.5546788,
            126.9706069,
            null);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(requestingUser));
    when(savedPlaceRepository.findById(11L)).thenReturn(Optional.of(foreignPlace));
    when(savedPlaceRepository.findByIdAndPlaceListUserId(11L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.createCourse(1L, new CourseCreateRequest("서울 코스", List.of(11L))))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SAVED_PLACE_NOT_FOUND));

    verify(savedPlaceRepository).findByIdAndPlaceListUserId(11L, 1L);
    verify(savedPlaceRepository, never()).findById(11L);
    verify(courseRepository, never()).save(any(Course.class));
  }
}
