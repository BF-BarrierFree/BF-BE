package com.barrierfree.bf.inquiry.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.inquiry.domain.InquiryCategory;
import com.barrierfree.bf.inquiry.dto.InquiryAnswerRequest;
import com.barrierfree.bf.inquiry.dto.InquiryCreateRequest;
import com.barrierfree.bf.inquiry.dto.InquiryResponse;
import com.barrierfree.bf.inquiry.dto.InquirySummaryResponse;
import com.barrierfree.bf.inquiry.entity.Inquiry;
import com.barrierfree.bf.inquiry.repository.InquiryRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("id", "category", "title", "createdAt", "updatedAt", "answeredAt");

  private final InquiryRepository inquiryRepository;
  private final UserRepository userRepository;

  @Transactional
  public InquiryResponse createInquiry(Long userId, InquiryCreateRequest request) {
    User user = getUser(userId);
    Inquiry inquiry =
        Inquiry.builder()
            .user(user)
            .category(InquiryCategory.from(request.category()))
            .title(normalizeRequiredText(request.title()))
            .content(normalizeRequiredText(request.content()))
            .build();

    return InquiryResponse.from(inquiryRepository.save(inquiry));
  }

  public Page<InquirySummaryResponse> getMyInquiries(Long userId, Pageable pageable) {
    getUser(userId);
    return inquiryRepository
        .findAllByUserId(userId, sanitizePageable(pageable))
        .map(InquirySummaryResponse::from);
  }

  public InquiryResponse getMyInquiry(Long userId, Long inquiryId) {
    getUser(userId);
    Inquiry inquiry =
        inquiryRepository
            .findByIdAndUserId(inquiryId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));

    return InquiryResponse.from(inquiry);
  }

  @Transactional
  public InquiryResponse answerInquiry(Long adminId, Long inquiryId, InquiryAnswerRequest request) {
    User admin = getUser(adminId);
    Inquiry inquiry = findInquiry(inquiryId);
    inquiry.answer(normalizeRequiredText(request.answer()), admin);
    return InquiryResponse.from(inquiry);
  }

  private Inquiry findInquiry(Long inquiryId) {
    return inquiryRepository
        .findById(inquiryId)
        .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
  }

  private User getUser(Long userId) {
    return userRepository
        .findByIdAndIsDeletedFalse(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  private String normalizeRequiredText(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return value.trim();
  }

  private Pageable sanitizePageable(Pageable pageable) {
    int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);
    Sort safeSort =
        Sort.by(
            pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .toList());

    if (safeSort.isUnsorted()) {
      safeSort = Sort.by(Sort.Direction.DESC, "createdAt");
    }

    return PageRequest.of(pageable.getPageNumber(), size, safeSort);
  }
}
