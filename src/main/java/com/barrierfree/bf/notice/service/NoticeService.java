package com.barrierfree.bf.notice.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.notice.domain.NoticeCategory;
import com.barrierfree.bf.notice.dto.NoticeCategoryResponse;
import com.barrierfree.bf.notice.dto.NoticeCreateRequest;
import com.barrierfree.bf.notice.dto.NoticeResponse;
import com.barrierfree.bf.notice.dto.NoticeUpdateRequest;
import com.barrierfree.bf.notice.entity.Notice;
import com.barrierfree.bf.notice.repository.NoticeRepository;
import java.util.Arrays;
import java.util.List;
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
public class NoticeService {

  private final NoticeRepository noticeRepository;
  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of("id", "category", "title", "createdAt", "updatedAt");

  public List<NoticeCategoryResponse> getCategories() {
    return Arrays.stream(NoticeCategory.values()).map(NoticeCategoryResponse::from).toList();
  }

  public Page<NoticeResponse> getNotices(String category, Pageable pageable) {
    NoticeCategory noticeCategory =
        category == null || category.isBlank() ? NoticeCategory.ALL : NoticeCategory.from(category);
    Pageable safePageable = sanitizePageable(pageable);

    Page<Notice> notices =
        noticeCategory == NoticeCategory.ALL
            ? noticeRepository.findActiveNotices(safePageable)
            : noticeRepository.findActiveNoticesByCategory(noticeCategory, safePageable);

    return notices.map(NoticeResponse::from);
  }

  public NoticeResponse getNotice(Long noticeId) {
    return NoticeResponse.from(findNotice(noticeId));
  }

  @Transactional
  public NoticeResponse createNotice(NoticeCreateRequest request) {
    Notice notice =
        Notice.builder()
            .category(NoticeCategory.writableFrom(request.category()))
            .title(normalizeRequiredText(request.title()))
            .content(normalizeRequiredText(request.content()))
            .build();

    return NoticeResponse.from(noticeRepository.save(notice));
  }

  @Transactional
  public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
    Notice notice = findNotice(noticeId);
    notice.update(
        NoticeCategory.writableFrom(request.category()),
        normalizeRequiredText(request.title()),
        normalizeRequiredText(request.content()));
    return NoticeResponse.from(notice);
  }

  @Transactional
  public void deleteNotice(Long noticeId) {
    findNotice(noticeId).softDelete();
  }

  private Notice findNotice(Long noticeId) {
    return noticeRepository
        .findByIdAndIsDeletedFalse(noticeId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));
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
