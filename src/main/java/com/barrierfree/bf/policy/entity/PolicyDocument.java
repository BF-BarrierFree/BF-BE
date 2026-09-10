package com.barrierfree.bf.policy.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.policy.domain.PolicyCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "policy_documents",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_policy_documents_category_version",
            columnNames = {"category", "version"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyDocument extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 100)
  private PolicyCategory category;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false)
  private boolean isActive = true;

  @Column(nullable = false)
  private Integer version = 1;

  @Column(nullable = false)
  private Integer displayOrder;

  @Column(nullable = false)
  private LocalDateTime effectiveDate;

  @Builder
  public PolicyDocument(
      PolicyCategory category,
      String title,
      String content,
      boolean isActive,
      Integer version,
      Integer displayOrder,
      LocalDateTime effectiveDate) {
    this.category = category;
    this.title = title;
    this.content = content;
    this.isActive = isActive;
    this.version = version == null ? 1 : version;
    this.displayOrder =
        displayOrder == null && category != null ? category.getDisplayOrder() : displayOrder;
    this.effectiveDate = effectiveDate == null ? LocalDateTime.now() : effectiveDate;
  }

  public void deactivate() {
    this.isActive = false;
  }
}
