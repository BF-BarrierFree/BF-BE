package com.barrierfree.bf.inquiry.dto;

import com.barrierfree.bf.inquiry.domain.InquiryCategory;
import com.barrierfree.bf.inquiry.domain.InquiryStatus;
import com.barrierfree.bf.inquiry.entity.Inquiry;
import java.time.LocalDateTime;

public record InquirySummaryResponse(
    Long id,
    InquiryCategory category,
    String categoryText,
    String title,
    InquiryStatus status,
    String statusText,
    LocalDateTime createdAt,
    LocalDateTime answeredAt) {

  public static InquirySummaryResponse from(Inquiry inquiry) {
    return new InquirySummaryResponse(
        inquiry.getId(),
        inquiry.getCategory(),
        inquiry.getCategory().getText(),
        inquiry.getTitle(),
        inquiry.getStatus(),
        inquiry.getStatus().getText(),
        inquiry.getCreatedAt(),
        inquiry.getAnsweredAt());
  }
}
