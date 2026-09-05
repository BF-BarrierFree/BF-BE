package com.barrierfree.bf.inquiry.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.inquiry.domain.InquiryCategory;
import com.barrierfree.bf.inquiry.domain.InquiryStatus;
import com.barrierfree.bf.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "inquiries",
    indexes = {
      @Index(name = "idx_inquiry_user_created_at", columnList = "user_id, created_at"),
      @Index(name = "idx_inquiry_created_at", columnList = "created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private InquiryCategory category;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InquiryStatus status = InquiryStatus.PENDING;

  @Column(columnDefinition = "TEXT")
  private String answer;

  private LocalDateTime answeredAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answerer_id")
  private User answerer;

  @Builder
  public Inquiry(User user, InquiryCategory category, String title, String content) {
    this.user = user;
    this.category = category;
    this.title = title;
    this.content = content;
  }

  public boolean isAnswered() {
    return status == InquiryStatus.ANSWERED;
  }

  public void answer(String answer, User answerer) {
    this.status = InquiryStatus.ANSWERED;
    this.answer = answer;
    this.answerer = answerer;
    this.answeredAt = LocalDateTime.now();
  }
}
