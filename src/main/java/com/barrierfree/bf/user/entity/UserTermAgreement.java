package com.barrierfree.bf.user.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저와 약관 간의 동의 내역을 관리하는 매핑 테이블 */
@Entity
@Table(name = "user_term_agreements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermAgreement extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "term_id", nullable = false)
  private Term term;

  @Column(nullable = false)
  private boolean isAgreed; // 동의 여부 (선택 약관의 경우 미동의 이력을 남길 수도 있음)

  @Builder
  public UserTermAgreement(User user, Term term, boolean isAgreed) {
    this.user = user;
    this.term = term;
    this.isAgreed = isAgreed;
  }

    // 약관 동의 상태 변경을 위한 편의 메서드 추가
    public void updateAgreement(boolean isAgreed) {
        this.isAgreed = isAgreed;
    }
}
