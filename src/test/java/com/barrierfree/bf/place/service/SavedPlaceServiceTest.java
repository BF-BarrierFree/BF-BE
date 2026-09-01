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
import com.barrierfree.bf.place.dto.PlaceDetailResponse;
import com.barrierfree.bf.place.dto.SavedPlaceCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListUpdateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceResponse;
import com.barrierfree.bf.place.entity.SavedPlace;
import com.barrierfree.bf.place.entity.SavedPlaceList;
import com.barrierfree.bf.place.repository.SavedPlaceListRepository;
import com.barrierfree.bf.place.repository.SavedPlaceRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class SavedPlaceServiceTest {

  private final SavedPlaceListRepository savedPlaceListRepository =
      Mockito.mock(SavedPlaceListRepository.class);
  private final SavedPlaceRepository savedPlaceRepository = Mockito.mock(SavedPlaceRepository.class);
  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final PlaceService placeService = Mockito.mock(PlaceService.class);
  private final SavedPlaceService service =
      new SavedPlaceService(
          savedPlaceListRepository, savedPlaceRepository, userRepository, placeService);

  @Test
  void savesPlaceSnapshotInUserList() {
    User user =
        User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "favorites");
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
                "Station",
                "TRANSPORTATION",
                true,
                "Seoul",
                37.5546788,
                126.9706069,
                "https://example.com/photo.jpg"));

    assertThat(response.placeId()).isEqualTo("place-1");
    assertThat(response.name()).isEqualTo("Station");
    assertThat(response.category()).isEqualTo(PlaceCategory.TRANSPORTATION);
    assertThat(response.openNow()).isTrue();
    assertThat(response.lat()).isEqualTo(37.5546788);
    assertThat(response.lng()).isEqualTo(126.9706069);
    assertThat(response.photoUrl()).isEqualTo("https://example.com/photo.jpg");
  }

  @Test
  void rejectsInvalidLocationWithoutSavingPlace() {
    User user =
        User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "favorites");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceListRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(placeList));

    assertThatThrownBy(
            () ->
                service.savePlace(
                    1L,
                    10L,
                    new SavedPlaceCreateRequest(
                        "place-1", "Station", "TRANSPORTATION", true, "Seoul", 91.0, 126.9, null)))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

    verify(savedPlaceRepository, never()).save(any(SavedPlace.class));
  }

  @Test
  void refreshesSavedPlaceSnapshotWhenReadingList() {
    User user =
        User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "favorites");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    SavedPlace savedPlace =
        new SavedPlace(
            placeList,
            "place-1",
            "old-name",
            PlaceCategory.ETC,
            false,
            "old-address",
            37.0,
            127.0,
            "https://example.com/old.jpg");
    ReflectionTestUtils.setField(savedPlace, "id", 1L);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceListRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(placeList));
    when(savedPlaceRepository.findAllByPlaceListIdOrderByCreatedAtDesc(10L))
        .thenReturn(List.of(savedPlace));
    when(placeService.getDetail("place-1"))
        .thenReturn(
            new PlaceDetailResponse(
                "place-1",
                "new-name",
                "new-address",
                37.1,
                127.1,
                PlaceCategory.TRANSPORTATION,
                PlaceCategory.TRANSPORTATION.getLabel(),
                null,
                null,
                true,
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
                "GOOGLE_PLACES_ONLY",
                "https://example.com/new.jpg"));

    SavedPlaceResponse response = service.getSavedPlaces(1L, 10L);

    assertThat(response.places()).hasSize(1);
    assertThat(response.places().getFirst().name()).isEqualTo("new-name");
    assertThat(response.places().getFirst().address()).isEqualTo("new-address");
    assertThat(response.places().getFirst().category()).isEqualTo(PlaceCategory.TRANSPORTATION);
    assertThat(response.places().getFirst().openNow()).isTrue();
    assertThat(response.places().getFirst().photoUrl()).isEqualTo("https://example.com/new.jpg");
  }

  @Test
  void removesSavedPlaceWhenSourcePlaceNoLongerExists() {
    User user =
        User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "favorites");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    SavedPlace savedPlace =
        new SavedPlace(
            placeList,
            "place-1",
            "name",
            PlaceCategory.ETC,
            false,
            "address",
            37.0,
            127.0,
            null);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceListRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(placeList));
    when(savedPlaceRepository.findAllByPlaceListIdOrderByCreatedAtDesc(10L))
        .thenReturn(List.of(savedPlace));
    when(placeService.getDetail("place-1"))
        .thenThrow(new CustomException(ErrorCode.FACILITY_NOT_FOUND));

    SavedPlaceResponse response = service.getSavedPlaces(1L, 10L);

    assertThat(response.places()).isEmpty();
    verify(savedPlaceRepository).delete(savedPlace);
  }

  @Test
  void updatesSavedPlaceListName() {
    User user =
        User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "favorites");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceListRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(placeList));

    var response = service.updateList(1L, 10L, new SavedPlaceListUpdateRequest("weekend"));

    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.name()).isEqualTo("weekend");
  }

  @Test
  void deletesSavedPlaceListWithChildren() {
    User user =
        User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "favorites");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    SavedPlace savedPlace =
        new SavedPlace(
            placeList,
            "place-1",
            "name",
            PlaceCategory.ETC,
            false,
            "address",
            37.0,
            127.0,
            null);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceListRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(placeList));
    when(savedPlaceRepository.findAllByPlaceListIdOrderByCreatedAtDesc(10L))
        .thenReturn(List.of(savedPlace));

    service.deleteList(1L, 10L);

    verify(savedPlaceRepository).deleteAll(List.of(savedPlace));
    verify(savedPlaceListRepository).delete(placeList);
  }

  @Test
  void removesSavedPlaceFromList() {
    User user =
        User.builder().socialId("kakao-1").nickname("tester").role(Role.USER).build();
    SavedPlaceList placeList = new SavedPlaceList(user, "favorites");
    ReflectionTestUtils.setField(placeList, "id", 10L);

    SavedPlace savedPlace =
        new SavedPlace(
            placeList,
            "place-1",
            "name",
            PlaceCategory.ETC,
            false,
            "address",
            37.0,
            127.0,
            null);
    ReflectionTestUtils.setField(savedPlace, "id", 7L);

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(savedPlaceRepository.findByIdAndPlaceListIdAndPlaceListUserId(7L, 10L, 1L))
        .thenReturn(Optional.of(savedPlace));

    service.removeSavedPlace(1L, 10L, 7L);

    verify(savedPlaceRepository).delete(savedPlace);
  }
}
