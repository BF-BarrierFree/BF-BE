package com.barrierfree.bf.place.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceDetailResponse;
import com.barrierfree.bf.place.dto.SavedPlaceCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListResponse;
import com.barrierfree.bf.place.dto.SavedPlaceListUpdateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceResponse;
import com.barrierfree.bf.place.entity.SavedPlace;
import com.barrierfree.bf.place.entity.SavedPlaceList;
import com.barrierfree.bf.place.repository.SavedPlaceListRepository;
import com.barrierfree.bf.place.repository.SavedPlaceRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedPlaceService {

  private final SavedPlaceListRepository savedPlaceListRepository;
  private final SavedPlaceRepository savedPlaceRepository;
  private final UserRepository userRepository;
  private final PlaceService placeService;

  @Value("${google.places.api-key}")
  private String googleApiKey;

  @Transactional
  public SavedPlaceListResponse.SavedPlaceListSummary createList(
      Long userId, SavedPlaceListCreateRequest request) {
    User user = findUser(userId);
    String name = normalizeRequiredText(request.name());

    SavedPlaceList placeList = savedPlaceListRepository.save(new SavedPlaceList(user, name));
    return toListSummary(placeList);
  }

  @Transactional(readOnly = true)
  public SavedPlaceListResponse getLists(Long userId) {
    findUser(userId);
    return new SavedPlaceListResponse(
        savedPlaceListRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toListSummary)
            .toList());
  }

  @Transactional
  public SavedPlaceListResponse.SavedPlaceListSummary updateList(
      Long userId, Long listId, SavedPlaceListUpdateRequest request) {
    SavedPlaceList placeList = findPlaceList(userId, listId);
    placeList.updateName(normalizeRequiredText(request.name()));
    return toListSummary(placeList);
  }

  @Transactional
  public void deleteList(Long userId, Long listId) {
    SavedPlaceList placeList = findPlaceList(userId, listId);
    savedPlaceRepository.deleteAll(
        savedPlaceRepository.findAllByPlaceListIdOrderByCreatedAtDesc(placeList.getId()));
    savedPlaceListRepository.delete(placeList);
  }

  @Transactional
  public SavedPlaceResponse.SavedPlaceSummary savePlace(
      Long userId, Long listId, SavedPlaceCreateRequest request) {
    SavedPlaceList placeList = findPlaceList(userId, listId);
    validateLocation(request.lat(), request.lng());

    String placeId = normalizeRequiredText(request.placeId());
    String name = normalizeRequiredText(request.name());
    PlaceCategory category = PlaceCategory.from(request.category());
    SavedPlace savedPlace =
        savedPlaceRepository
            .findByPlaceListIdAndPlaceId(placeList.getId(), placeId)
            .orElseGet(
                () ->
                    new SavedPlace(
                        placeList,
                        placeId,
                        name,
                        category,
                        request.openNow(),
                        normalizeOptionalText(request.address()),
                        request.lat(),
                        request.lng(),
                        normalizePhotoUrl(request.photoUrl())));

    savedPlace.updateSnapshot(
        name,
        category,
        request.openNow(),
        normalizeOptionalText(request.address()),
        request.lat(),
        request.lng(),
        normalizePhotoUrl(request.photoUrl()));

    return toPlaceSummary(savedPlaceRepository.save(savedPlace));
  }

  @Transactional
  public SavedPlaceResponse getSavedPlaces(Long userId, Long listId) {
    SavedPlaceList placeList = findPlaceList(userId, listId);
    return new SavedPlaceResponse(
        savedPlaceRepository.findAllByPlaceListIdOrderByCreatedAtDesc(placeList.getId()).stream()
            .map(this::refreshSavedPlace)
            .filter(savedPlace -> savedPlace != null)
            .map(this::toPlaceSummary)
            .toList());
  }

  @Transactional
  public void removeSavedPlace(Long userId, Long listId, Long savedPlaceId) {
    SavedPlaceList placeList = findPlaceList(userId, listId);
    SavedPlace savedPlace =
        savedPlaceRepository
            .findByIdAndPlaceListId(savedPlaceId, placeList.getId())
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    savedPlaceRepository.delete(savedPlace);
  }

  private SavedPlace refreshSavedPlace(SavedPlace savedPlace) {
    try {
      PlaceDetailResponse latest = placeService.getDetail(savedPlace.getPlaceId());
      savedPlace.updateSnapshot(
          latest.name(),
          latest.category(),
          latest.openNow(),
          latest.address(),
          latest.lat(),
          latest.lng(),
          normalizePhotoUrl(latest.photoUrl()));
      return savedPlace;
    } catch (CustomException exception) {
      if (exception.getErrorCode() == ErrorCode.FACILITY_NOT_FOUND) {
        savedPlaceRepository.delete(savedPlace);
        return null;
      }
      return savedPlace;
    }
  }

  private User findUser(Long userId) {
    if (userId == null) {
      throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }
    return userRepository
        .findByIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  private SavedPlaceList findPlaceList(Long userId, Long listId) {
    findUser(userId);
    return savedPlaceListRepository
        .findByIdAndUserId(listId, userId)
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
  }

  private void validateLocation(Double latitude, Double longitude) {
    if (latitude == null && longitude == null) {
      return;
    }
    if (!isLatitude(latitude) || !isLongitude(longitude)) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private boolean isLatitude(Double value) {
    return value != null && Double.isFinite(value) && value >= -90.0 && value <= 90.0;
  }

  private boolean isLongitude(Double value) {
    return value != null && Double.isFinite(value) && value >= -180.0 && value <= 180.0;
  }

  private String normalizeRequiredText(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return value.trim();
  }

  private String normalizeOptionalText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizePhotoUrl(String value) {
    String photoUrl = normalizeOptionalText(value);
    if (photoUrl == null) {
      return null;
    }
    if ((googleApiKey != null && !googleApiKey.isBlank() && photoUrl.contains(googleApiKey))
        || (photoUrl.contains("places.googleapis.com") && photoUrl.contains("key="))) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return photoUrl;
  }

  private SavedPlaceListResponse.SavedPlaceListSummary toListSummary(SavedPlaceList placeList) {
    return new SavedPlaceListResponse.SavedPlaceListSummary(
        placeList.getId(), placeList.getName(), placeList.getCreatedAt());
  }

  private SavedPlaceResponse.SavedPlaceSummary toPlaceSummary(SavedPlace savedPlace) {
    PlaceCategory category = savedPlace.getCategory();
    return new SavedPlaceResponse.SavedPlaceSummary(
        savedPlace.getId(),
        savedPlace.getPlaceId(),
        savedPlace.getName(),
        category,
        category.getLabel(),
        savedPlace.getOpenNow(),
        savedPlace.getAddress(),
        savedPlace.getLatitude(),
        savedPlace.getLongitude(),
        savedPlace.getPhotoUrl(),
        savedPlace.getCreatedAt());
  }
}
