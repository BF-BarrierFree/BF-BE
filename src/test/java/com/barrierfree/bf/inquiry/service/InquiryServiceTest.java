package com.barrierfree.bf.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.inquiry.domain.InquiryCategory;
import com.barrierfree.bf.inquiry.domain.InquiryStatus;
import com.barrierfree.bf.inquiry.dto.InquiryAnswerRequest;
import com.barrierfree.bf.inquiry.dto.InquiryCreateRequest;
import com.barrierfree.bf.inquiry.dto.InquiryResponse;
import com.barrierfree.bf.inquiry.entity.Inquiry;
import com.barrierfree.bf.inquiry.repository.InquiryRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InquiryServiceTest {

  private final InquiryRepository inquiryRepository = Mockito.mock(InquiryRepository.class);
  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final InquiryService service = new InquiryService(inquiryRepository, userRepository);

  @Test
  void createsInquiryWithFlexibleKoreanCategory() {
    User user = User.builder().socialId("kakao-1").nickname("user").role(Role.USER).build();

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(inquiryRepository.save(any(Inquiry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InquiryResponse response =
        service.createInquiry(
            1L, new InquiryCreateRequest("서비스이용", " 문의 제목 ", " 문의 내용 "));

    assertThat(response.category()).isEqualTo(InquiryCategory.SERVICE_USAGE);
    assertThat(response.status()).isEqualTo(InquiryStatus.PENDING);
    assertThat(response.title()).isEqualTo("문의 제목");
    assertThat(response.content()).isEqualTo("문의 내용");
  }

  @Test
  void getsOnlyInquiryOwnedByRequestingUser() {
    User user = User.builder().socialId("kakao-1").nickname("user").role(Role.USER).build();
    Inquiry inquiry =
        Inquiry.builder()
            .user(user)
            .category(InquiryCategory.SERVICE_USAGE)
            .title("문의 제목")
            .content("문의 내용")
            .build();

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(inquiryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(inquiry));

    InquiryResponse response = service.getMyInquiry(1L, 10L);

    assertThat(response.title()).isEqualTo("문의 제목");
    verify(inquiryRepository).findByIdAndUserId(10L, 1L);
  }

  @Test
  void rejectsInquiryDetailWhenNotOwnedByRequestingUser() {
    User user = User.builder().socialId("kakao-1").nickname("user").role(Role.USER).build();

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(inquiryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getMyInquiry(1L, 10L))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND));
  }

  @Test
  void answersInquiryByAdmin() {
    User admin = User.builder().socialId("kakao-admin").nickname("admin").role(Role.ADMIN).build();
    User user = User.builder().socialId("kakao-1").nickname("user").role(Role.USER).build();
    Inquiry inquiry =
        Inquiry.builder()
            .user(user)
            .category(InquiryCategory.CONTENT)
            .title("콘텐츠 문의")
            .content("내용")
            .build();

    when(userRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.of(admin));
    when(inquiryRepository.findById(10L)).thenReturn(Optional.of(inquiry));

    InquiryResponse response =
        service.answerInquiry(99L, 10L, new InquiryAnswerRequest(" 답변 내용 "));

    assertThat(response.answer()).isEqualTo("답변 내용");
    assertThat(response.status()).isEqualTo(InquiryStatus.ANSWERED);
    assertThat(response.answeredAt()).isNotNull();
  }
}
