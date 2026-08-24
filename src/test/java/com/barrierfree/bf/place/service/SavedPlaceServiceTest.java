package com.barrierfree.bf.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.SavedPlaceCreateRequest;
import com.barrierfree.bf.place.entity.SavedPlace;
import com.barrierfree.bf.place.entity.SavedPlaceList;
import com.barrierfree.bf.place.repository.SavedPlaceListRepository;
import com.barrierfree.bf.place.repository.SavedPlaceRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class SavedPlaceServiceTest {

  private final SavedPlaceListRepository savedPlaceListRepository =
      Mockito.mock(SavedPlaceListRepository.class);
  private final SavedPlaceRepository savedPlaceRepository =
      Mockito.mock(SavedPlaceRepository.class);
  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final SavedPlaceService service =
      new SavedPlaceService(savedPlaceListRepository, savedPlaceRepository, userRepository);

  @Test
  void savesPlaceSnapshotInUserList() {
    User user = User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "가고 싶은 곳");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceListRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(placeList));
    when(savedPlaceRepository.findByPlaceListIdAndPlaceId(10L, "place-1"))
        .thenReturn(Optional.empty());
    when(savedPlaceRepository.save(any(SavedPlace.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        service.savePlace(
            1L,
            10L,
            new SavedPlaceCreateRequest(
                " place-1 ",
                " 서울역 ",
                "TRANSPORTATION",
                true,
                "서울 중구",
                37.5546788,
                126.9706069,
                "https://example.com/photo.jpg"));

    assertThat(response.placeId()).isEqualTo("place-1");
    assertThat(response.name()).isEqualTo("서울역");
    assertThat(response.category()).isEqualTo(PlaceCategory.TRANSPORTATION);
    assertThat(response.openNow()).isTrue();
    assertThat(response.lat()).isEqualTo(37.5546788);
    assertThat(response.lng()).isEqualTo(126.9706069);
    assertThat(response.photoUrl()).isEqualTo("https://example.com/photo.jpg");
  }

  @Test
  void rejectsInvalidLocationWithoutSavingPlace() {
    User user = User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "가고 싶은 곳");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceListRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(placeList));

    assertThatThrownBy(
            () ->
                service.savePlace(
                    1L,
                    10L,
                    new SavedPlaceCreateRequest(
                        "place-1", "서울역", "TRANSPORTATION", true, "서울 중구", 91.0, 126.9, null)))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

    verify(savedPlaceRepository, never()).save(any(SavedPlace.class));
  }
}
