package com.barrierfree.bf.review.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 도움이 되었다고 표시한 리뷰입니다. 사용자별 중복 표시는 허용하지 않습니다. */
@Entity
@Table(
    name = "review_helpfuls",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_review_helpful_user_review",
            columnNames = {"user_id", "review_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewHelpful extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "review_id", nullable = false)
  private Review review;

  public ReviewHelpful(User user, Review review) {
    this.user = user;
    this.review = review;
  }
}
