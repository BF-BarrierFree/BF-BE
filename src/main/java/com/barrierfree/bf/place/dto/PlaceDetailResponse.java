package com.barrierfree.bf.place.dto;

import com.barrierfree.bf.place.domain.PlaceCategory;

public record PlaceDetailResponse(
    String placeId,
    String name,
    String address,
    Double latitude,
    Double longitude,
    PlaceCategory category,
    String categoryLabel,
    String phoneNumber,
    String websiteUri,
    Boolean openNow,
    Integer reviewCount,
    Boolean wheelchairAccessibleEntrance,
    Boolean wheelchairAccessibleParking,
    Boolean wheelchairAccessibleRestroom,
    Boolean wheelchairAccessibleSeating,
    Boolean elevator,
    Boolean ramp,
    Boolean voiceGuidance,
    Boolean brailleBlock,
    Boolean hearingSupport,
    Boolean strollerRental,
    Boolean nursingRoom,
    Boolean wheelchairRental,
    Boolean signLanguage,
    Boolean restArea,
    Boolean subtitleService,
    String accessibilityDataSource,
    String photoUrl) {}
