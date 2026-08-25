package com.barrierfree.bf.notice.dto;

import com.barrierfree.bf.notice.domain.NoticeCategory;

public record NoticeCategoryResponse(String category, String defaultText, String selectedText) {

  public static NoticeCategoryResponse from(NoticeCategory category) {
    return new NoticeCategoryResponse(
        category.name(), category.getDefaultText(), category.getSelectedText());
  }
}
