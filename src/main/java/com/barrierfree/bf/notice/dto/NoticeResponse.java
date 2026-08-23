package com.barrierfree.bf.notice.dto;

import com.barrierfree.bf.notice.domain.NoticeCategory;
import com.barrierfree.bf.notice.entity.Notice;
import java.time.LocalDateTime;

public record NoticeResponse(
    Long id,
    NoticeCategory category,
    String categoryText,
    String title,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static NoticeResponse from(Notice notice) {
    return new NoticeResponse(
        notice.getId(),
        notice.getCategory(),
        notice.getCategory().getDefaultText(),
        notice.getTitle(),
        notice.getContent(),
        notice.getCreatedAt(),
        notice.getUpdatedAt());
  }
}
