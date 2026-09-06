package com.barrierfree.bf.inquiry.dto;

import com.barrierfree.bf.inquiry.domain.InquiryCategory;
import com.barrierfree.bf.inquiry.domain.InquiryStatus;
import com.barrierfree.bf.inquiry.entity.Inquiry;
import java.time.LocalDateTime;

public record InquiryResponse(
    Long id,
    InquiryCategory category,
    String categoryText,
    String title,
    String content,
    InquiryStatus status,
    String statusText,
    String answer,
    LocalDateTime answeredAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static InquiryResponse from(Inquiry inquiry) {
    return new InquiryResponse(
        inquiry.getId(),
        inquiry.getCategory(),
        inquiry.getCategory().getText(),
        inquiry.getTitle(),
        inquiry.getContent(),
        inquiry.getStatus(),
        inquiry.getStatus().getText(),
        inquiry.getAnswer(),
        inquiry.getAnsweredAt(),
        inquiry.getCreatedAt(),
        inquiry.getUpdatedAt());
  }
}
