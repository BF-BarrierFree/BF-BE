package com.barrierfree.bf.inquiry.domain;

public enum InquiryStatus {
  PENDING("답변 대기"),
  ANSWERED("답변 완료");

  private final String text;

  InquiryStatus(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
