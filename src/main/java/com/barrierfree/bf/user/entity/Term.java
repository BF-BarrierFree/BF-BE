package com.barrierfree.bf.user.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 약관 마스터 테이블 약관 내용 변경이나 필수/선택 여부 변동에 유연하게 대응하기 위해 분리합니다. 약관은 불변(immutable) 개정판(revision)으로
 * 관리되며, 변경 시 새로운 레코드를 생성합니다.
 */
@Entity
@Table(name = "terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title; // 약관 제목 (예: 서비스 이용약관)

  @Column(columnDefinition = "TEXT")
  private String content; // 약관 본문 (또는 노션 링크 등)

  @Column(nullable = false)
  private boolean isRequired; // 필수 동의 여부 (true: 필수, false: 선택)

  @Column(nullable = false)
  private boolean isActive = true; // 현재 사용 중인 약관인지 여부

  @Column(nullable = false)
  private Integer version = 1; // 약관 버전 (동일 약관의 개정 이력 추적용)

  @Column(nullable = false)
  private LocalDateTime effectiveDate; // 약관 시행일 (해당 버전이 유효해진 날짜)

  @Builder
  public Term(
      String title,
      String content,
      boolean isRequired,
      boolean isActive,
      Integer version,
      LocalDateTime effectiveDate) {
    this.title = title;
    this.content = content;
    this.isRequired = isRequired;
    this.isActive = isActive;
    this.version = version != null ? version : 1;
    this.effectiveDate = effectiveDate != null ? effectiveDate : LocalDateTime.now();
  }
}
