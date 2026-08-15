package com.barrierfree.bf.place.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.SavedPlaceCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListResponse;
import com.barrierfree.bf.place.dto.SavedPlaceResponse;
import com.barrierfree.bf.place.entity.SavedPlace;
import com.barrierfree.bf.place.entity.SavedPlaceList;
import com.barrierfree.bf.place.repository.SavedPlaceListRepository;
import com.barrierfree.bf.place.repository.SavedPlaceRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedPlaceService {

  private final SavedPlaceListRepository savedPlaceListRepository;
  private final SavedPlaceRepository savedPlaceRepository;
  private final UserRepository userRepository;

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
  public SavedPlaceResponse.SavedPlaceSummary savePlace(
      Long userId, Long listId, SavedPlaceCreateRequest request) {
    SavedPlaceList placeList = findPlaceList(userId, listId);
    validateLocation(request.latitude(), request.longitude());

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
                        request.latitude(),
                        request.longitude(),
                        normalizeOptionalText(request.photoUrl())));

    savedPlace.updateSnapshot(
        name,
        category,
        request.openNow(),
        normalizeOptionalText(request.address()),
        request.latitude(),
        request.longitude(),
        normalizeOptionalText(request.photoUrl()));

    return toPlaceSummary(savedPlaceRepository.save(savedPlace));
  }

  @Transactional(readOnly = true)
  public SavedPlaceResponse getSavedPlaces(Long userId, Long listId) {
    SavedPlaceList placeList = findPlaceList(userId, listId);
    return new SavedPlaceResponse(
        savedPlaceRepository.findAllByPlaceListIdOrderByCreatedAtDesc(placeList.getId()).stream()
            .map(this::toPlaceSummary)
            .toList());
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
