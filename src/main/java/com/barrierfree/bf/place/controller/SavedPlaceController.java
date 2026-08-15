package com.barrierfree.bf.place.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.place.dto.SavedPlaceCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListResponse;
import com.barrierfree.bf.place.dto.SavedPlaceResponse;
import com.barrierfree.bf.place.service.SavedPlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/place-lists")
public class SavedPlaceController {

  private final SavedPlaceService savedPlaceService;

  @PostMapping
  public ApiResponse<SavedPlaceListResponse.SavedPlaceListSummary> createList(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody SavedPlaceListCreateRequest request) {
    return ApiResponse.success(savedPlaceService.createList(userId, request));
  }

  @GetMapping
  public ApiResponse<SavedPlaceListResponse> getLists(@AuthenticationPrincipal Long userId) {
    return ApiResponse.success(savedPlaceService.getLists(userId));
  }

  @PostMapping("/{listId}/places")
  public ApiResponse<SavedPlaceResponse.SavedPlaceSummary> savePlace(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long listId,
      @Valid @RequestBody SavedPlaceCreateRequest request) {
    return ApiResponse.success(savedPlaceService.savePlace(userId, listId, request));
  }

  @GetMapping("/{listId}/places")
  public ApiResponse<SavedPlaceResponse> getSavedPlaces(
      @AuthenticationPrincipal Long userId, @PathVariable Long listId) {
    return ApiResponse.success(savedPlaceService.getSavedPlaces(userId, listId));
  }
}
