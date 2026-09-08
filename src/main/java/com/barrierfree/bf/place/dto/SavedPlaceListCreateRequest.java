package com.barrierfree.bf.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedPlaceListCreateRequest(
    @Schema(description = "Saved place list name", example = "favorites") @NotBlank @Size(max = 50)
        String name,
    @Schema(description = "Saved place list emoji", example = "\uD83D\uDCCD") @Size(max = 20)
        String emoji) {

  public SavedPlaceListCreateRequest(String name) {
    this(name, null);
  }
}
