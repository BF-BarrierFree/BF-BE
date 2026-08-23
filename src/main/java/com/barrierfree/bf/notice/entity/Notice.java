package com.barrierfree.bf.notice.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.notice.domain.NoticeCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "notices",
    indexes = {
      @Index(name = "idx_notice_category", columnList = "category"),
      @Index(name = "idx_notice_created_at", columnList = "createdAt")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NoticeCategory category;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false)
  private boolean isDeleted = false;

  private LocalDateTime deletedAt;

  @Builder
  public Notice(NoticeCategory category, String title, String content) {
    this.category = category;
    this.title = title;
    this.content = content;
  }

  public void update(NoticeCategory category, String title, String content) {
    this.category = category;
    this.title = title;
    this.content = content;
  }

  public void softDelete() {
    this.isDeleted = true;
    this.deletedAt = LocalDateTime.now();
  }
}
