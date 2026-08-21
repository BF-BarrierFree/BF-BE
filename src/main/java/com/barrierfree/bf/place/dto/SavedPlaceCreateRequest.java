package com.barrierfree.bf.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedPlaceCreateRequest(
    @Schema(description = "장소 ID", example = "ChIJ...")
    @NotBlank @Size(max = 150) String placeId,
    @Schema(description = "장소 이름", example = "서울역")
    @NotBlank @Size(max = 200) String name,
    @Schema(description = "장소 카테고리", example = "TRANSPORTATION")
    String category,
    @Schema(description = "현재 영업중 여부", example = "true")
    Boolean openNow,
    @Schema(description = "장소 주소", example = "서울 중구 한강대로 405")
    @Size(max = 500) String address,
    @Schema(description = "위도", example = "37.5546788")
    Double lat,
    @Schema(description = "경도", example = "126.9706069")
    Double lng,
    @Schema(description = "장소 사진 URL", example = "https://places.googleapis.com/v1/...")
    @Size(max = 1000) String photoUrl) {}
