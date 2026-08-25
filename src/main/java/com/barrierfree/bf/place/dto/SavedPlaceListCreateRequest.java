package com.barrierfree.bf.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedPlaceListCreateRequest(
    @Schema(description = "장소 저장 목록 이름", example = "가고 싶은 곳") @NotBlank @Size(max = 50)
        String name) {}
