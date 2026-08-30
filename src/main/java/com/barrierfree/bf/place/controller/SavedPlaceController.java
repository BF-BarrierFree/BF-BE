package com.barrierfree.bf.place.controller;

import com.barrierfree.bf.global.response.ApiResponse;
import com.barrierfree.bf.place.dto.SavedPlaceCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListCreateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceListResponse;
import com.barrierfree.bf.place.dto.SavedPlaceListUpdateRequest;
import com.barrierfree.bf.place.dto.SavedPlaceResponse;
import com.barrierfree.bf.place.service.SavedPlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/place-lists")
@Tag(name = "Saved Place", description = "장소 저장 목록 API")
public class SavedPlaceController {

  private final SavedPlaceService savedPlaceService;

  @Operation(summary = "저장 장소 목록 생성", description = "사용자가 지정한 이름으로 장소 저장 목록을 생성합니다.")
  @PostMapping
  public ApiResponse<SavedPlaceListResponse.SavedPlaceListSummary> createList(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Valid @RequestBody SavedPlaceListCreateRequest request) {
    return ApiResponse.success(savedPlaceService.createList(userId, request));
  }

  @Operation(summary = "저장 장소 목록 조회", description = "현재 로그인한 사용자의 장소 저장 목록을 조회합니다.")
  @GetMapping
  public ApiResponse<SavedPlaceListResponse> getLists(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
    return ApiResponse.success(savedPlaceService.getLists(userId));
  }

  @Operation(summary = "저장 장소 목록 이름 수정", description = "저장 장소 목록의 이름을 변경합니다.")
  @PatchMapping("/{listId}")
  public ApiResponse<SavedPlaceListResponse.SavedPlaceListSummary> updateList(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Parameter(description = "저장 장소 목록 ID", example = "1") @PathVariable Long listId,
      @Valid @RequestBody SavedPlaceListUpdateRequest request) {
    return ApiResponse.success(savedPlaceService.updateList(userId, listId, request));
  }

  @Operation(summary = "저장 장소 목록 삭제", description = "저장 장소 목록과 목록에 담긴 장소를 함께 삭제합니다.")
  @DeleteMapping("/{listId}")
  public ApiResponse<?> deleteList(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Parameter(description = "저장 장소 목록 ID", example = "1") @PathVariable Long listId) {
    savedPlaceService.deleteList(userId, listId);
    return ApiResponse.successWithNoContent();
  }

  @Operation(summary = "장소 저장", description = "검색 또는 상세조회 결과에서 선택한 장소를 특정 목록에 저장합니다.")
  @PostMapping("/{listId}/places")
  public ApiResponse<SavedPlaceResponse.SavedPlaceSummary> savePlace(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Parameter(description = "저장 장소 목록 ID", example = "1") @PathVariable Long listId,
      @Valid @RequestBody SavedPlaceCreateRequest request) {
    return ApiResponse.success(savedPlaceService.savePlace(userId, listId, request));
  }

  @Operation(summary = "저장 장소 조회", description = "특정 저장 장소 목록 안의 장소들을 조회합니다.")
  @GetMapping("/{listId}/places")
  public ApiResponse<SavedPlaceResponse> getSavedPlaces(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Parameter(description = "저장 장소 목록 ID", example = "1") @PathVariable Long listId) {
    return ApiResponse.success(savedPlaceService.getSavedPlaces(userId, listId));
  }

  @Operation(summary = "저장 장소 해제", description = "저장 목록에서 특정 장소를 제거합니다.")
  @DeleteMapping("/{listId}/places/{savedPlaceId}")
  public ApiResponse<?> removeSavedPlace(
      @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
      @Parameter(description = "저장 장소 목록 ID", example = "1") @PathVariable Long listId,
      @Parameter(description = "저장 장소 ID", example = "1") @PathVariable Long savedPlaceId) {
    savedPlaceService.removeSavedPlace(userId, listId, savedPlaceId);
    return ApiResponse.successWithNoContent();
  }
}
