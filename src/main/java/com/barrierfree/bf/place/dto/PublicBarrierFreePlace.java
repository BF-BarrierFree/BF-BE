package com.barrierfree.bf.place.dto;

public record PublicBarrierFreePlace(
    String contentId,
    String name,
    String address,
    Double latitude,
    Double longitude,
    PublicBarrierFreeInfo barrierFreeInfo) {}
