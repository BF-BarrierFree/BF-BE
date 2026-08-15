package com.barrierfree.bf.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedPlaceCreateRequest(
    @NotBlank @Size(max = 150) String placeId,
    @NotBlank @Size(max = 200) String name,
    String category,
    Boolean openNow,
    @Size(max = 500) String address,
    Double latitude,
    Double longitude,
    @Size(max = 1000) String photoUrl) {}
