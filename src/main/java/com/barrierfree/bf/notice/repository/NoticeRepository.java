package com.barrierfree.bf.notice.repository;

import com.barrierfree.bf.notice.domain.NoticeCategory;
import com.barrierfree.bf.notice.entity.Notice;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

  Optional<Notice> findByIdAndIsDeletedFalse(Long id);

  @Query("SELECT n FROM Notice n WHERE n.isDeleted = false")
  Page<Notice> findActiveNotices(Pageable pageable);

  @Query("SELECT n FROM Notice n WHERE n.isDeleted = false AND n.category = :category")
  Page<Notice> findActiveNoticesByCategory(
      @Param("category") NoticeCategory category, Pageable pageable);
}
