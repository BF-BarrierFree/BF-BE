package com.barrierfree.bf.taxi.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.taxi.dto.TaxiReservationRequest;
import com.barrierfree.bf.taxi.entity.TaxiCenter;
import com.barrierfree.bf.taxi.entity.TaxiReservation;
import com.barrierfree.bf.taxi.repository.TaxiCenterRepository;
import com.barrierfree.bf.taxi.repository.TaxiReservationRepository;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TaxiReservationServiceTest {
  private final TaxiCenterRepository centers = mock(TaxiCenterRepository.class);
  private final TaxiReservationRepository reservations = mock(TaxiReservationRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final TaxiReservationService service =
      new TaxiReservationService(centers, reservations, users, mock(OpenRouterService.class));

  @Test
  void persistsExistingReservationFieldsForActiveUser() {
    User user = User.builder().socialId("qa").nickname("tester").role(Role.GUEST).build();
    TaxiCenter center = TaxiCenter.builder().centerId("center").build();
    when(users.findByIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(user));
    when(centers.findById(1L)).thenReturn(Optional.of(center));
    service.saveReservationHistory(7L, request());
    ArgumentCaptor<TaxiReservation> captor = ArgumentCaptor.forClass(TaxiReservation.class);
    verify(reservations).save(captor.capture());
    TaxiReservation saved = captor.getValue();
    assertThat(saved.getUser()).isSameAs(user);
    assertThat(saved.getTaxiCenter()).isSameAs(center);
    assertThat(saved.getDepartureAddress()).isEqualTo("서울역");
    assertThat(saved.getDestinationAddress()).isEqualTo("시청");
    assertThat(saved.getEstimatedFare()).isEqualTo("2500원");
    assertThat(saved.getGeneratedMessage()).isEqualTo("예약 요청");
    assertThat(saved.getUserInputMetadata()).isEqualTo("{}");
    verify(users, never()).findById(anyLong());
  }

  @Test
  void refusesMissingOrWithdrawnUserBeforeSaving() {
    when(users.findByIdAndIsDeletedFalse(7L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.saveReservationHistory(7L, request()))
        .isInstanceOfSatisfying(
            CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    verifyNoInteractions(centers, reservations);
  }

  private TaxiReservationRequest.CreateReservation request() {
    var request = new TaxiReservationRequest.CreateReservation();
    ReflectionTestUtils.setField(request, "centerId", 1L);
    ReflectionTestUtils.setField(request, "startAddr", "서울역");
    ReflectionTestUtils.setField(request, "endAddr", "시청");
    ReflectionTestUtils.setField(request, "estimatedFare", "2500원");
    ReflectionTestUtils.setField(request, "generatedMessage", "예약 요청");
    ReflectionTestUtils.setField(request, "userMetadataJson", "{}");
    return request;
  }
}
