package com.barrierfree.bf.place.dto;

public record PublicBarrierFreeInfo(
    Boolean elevator,
    Boolean ramp,
    Boolean accessibleParking,
    Boolean wheelchairSeat,
    Boolean accessibleRestroom,
    Boolean voiceGuidance,
    Boolean brailleBlock,
    Boolean hearingSupport,
    Boolean strollerRental,
    Boolean nursingRoom,
    Boolean wheelchairRental,
    Boolean signLanguage,
    Boolean restArea,
    Boolean subtitleService,
    String sourceStatus) {

  public static PublicBarrierFreeInfo empty() {
    return notFound();
  }

  public static PublicBarrierFreeInfo notFound() {
    return new PublicBarrierFreeInfo(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        "PUBLIC_DATA_NOT_FOUND");
  }

  public static PublicBarrierFreeInfo apiFailed() {
    return new PublicBarrierFreeInfo(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        "PUBLIC_DATA_API_FAILED");
  }

  public boolean hasAnyValue() {
    return elevator != null
        || ramp != null
        || accessibleParking != null
        || wheelchairSeat != null
        || accessibleRestroom != null
        || voiceGuidance != null
        || brailleBlock != null
        || hearingSupport != null
        || strollerRental != null
        || nursingRoom != null
        || wheelchairRental != null
        || signLanguage != null
        || restArea != null
        || subtitleService != null;
  }
}
