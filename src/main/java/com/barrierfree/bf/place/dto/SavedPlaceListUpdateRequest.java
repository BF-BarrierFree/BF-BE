package com.barrierfree.bf.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedPlaceListUpdateRequest(
    @Schema(description = "저장 장소 목록 이름", example = "favorites") @NotBlank @Size(max = 50)
        String name) {}
