package com.barrierfree.bf.inquiry.repository;

import com.barrierfree.bf.inquiry.entity.Inquiry;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

  Page<Inquiry> findAllByUserId(Long userId, Pageable pageable);

  Optional<Inquiry> findByIdAndUserId(Long id, Long userId);
}
