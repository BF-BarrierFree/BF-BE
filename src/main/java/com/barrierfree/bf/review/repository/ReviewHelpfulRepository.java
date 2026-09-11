package com.barrierfree.bf.review.repository;

import com.barrierfree.bf.review.entity.ReviewHelpful;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, Long> {

  interface HelpfulCount {
    Long getReviewId();

    long getCount();
  }

  @Query(
      "SELECT h.review.id AS reviewId, COUNT(h) AS count FROM ReviewHelpful h WHERE h.review.id IN :reviewIds GROUP BY h.review.id")
  List<HelpfulCount> countByReviewIds(@Param("reviewIds") List<Long> reviewIds);

  boolean existsByUserIdAndReviewId(Long userId, Long reviewId);

  Optional<ReviewHelpful> findByUserIdAndReviewId(Long userId, Long reviewId);

  @Query(
      value =
          "SELECT h FROM ReviewHelpful h JOIN FETCH h.review r JOIN FETCH r.user "
              + "WHERE h.user.id = :userId AND r.isDeleted = false",
      countQuery =
          "SELECT COUNT(h) FROM ReviewHelpful h JOIN h.review r "
              + "WHERE h.user.id = :userId AND r.isDeleted = false")
  Page<ReviewHelpful> findActiveHelpfulReviewsByUserId(
      @Param("userId") Long userId, Pageable pageable);
}
