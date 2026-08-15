package com.barrierfree.bf.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedPlaceListCreateRequest(
    @NotBlank @Size(max = 50) String name) {}
