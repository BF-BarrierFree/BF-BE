package com.barrierfree.bf.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.user.dto.TermAgreementUpdateRequest;
import com.barrierfree.bf.user.entity.Term;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.entity.UserTermAgreement;
import com.barrierfree.bf.user.repository.TermRepository;
import com.barrierfree.bf.user.repository.UserRepository;
import com.barrierfree.bf.user.repository.UserTermAgreementRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserTermAgreementUpdateExecutorTest {

  @Test
  void refetchesAndUpdatesExistingAgreement() {
    UserRepository userRepository = mock(UserRepository.class);
    TermRepository termRepository = mock(TermRepository.class);
    UserTermAgreementRepository agreementRepository = mock(UserTermAgreementRepository.class);
    UserTermAgreementUpdateExecutor executor =
        new UserTermAgreementUpdateExecutor(userRepository, termRepository, agreementRepository);
    TermAgreementUpdateRequest request = mock(TermAgreementUpdateRequest.class);
    TermAgreementUpdateRequest.TermAgreementDto agreementDto =
        mock(TermAgreementUpdateRequest.TermAgreementDto.class);
    User user = mock(User.class);
    Term term = mock(Term.class);
    UserTermAgreement existingAgreement = mock(UserTermAgreement.class);

    when(request.getAgreements()).thenReturn(List.of(agreementDto));
    when(agreementDto.getTermId()).thenReturn(11L);
    when(agreementDto.getIsAgreed()).thenReturn(false);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(termRepository.findById(11L)).thenReturn(Optional.of(term));
    when(term.isActive()).thenReturn(true);
    when(term.getId()).thenReturn(11L);
    when(agreementRepository.findByUserIdAndTermId(7L, 11L))
        .thenReturn(Optional.of(existingAgreement));

    executor.updateAgreements(7L, request);

    verify(existingAgreement).updateAgreement(false);
    verify(agreementRepository).save(existingAgreement);
  }

  @Test
  void rejectsDuplicateTermIdsInOneRequest() {
    UserRepository userRepository = mock(UserRepository.class);
    TermRepository termRepository = mock(TermRepository.class);
    UserTermAgreementRepository agreementRepository = mock(UserTermAgreementRepository.class);
    UserTermAgreementUpdateExecutor executor =
        new UserTermAgreementUpdateExecutor(userRepository, termRepository, agreementRepository);
    TermAgreementUpdateRequest request = mock(TermAgreementUpdateRequest.class);
    TermAgreementUpdateRequest.TermAgreementDto agreementDto =
        mock(TermAgreementUpdateRequest.TermAgreementDto.class);
    User user = mock(User.class);

    when(request.getAgreements()).thenReturn(List.of(agreementDto, agreementDto));
    when(agreementDto.getTermId()).thenReturn(11L);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    CustomException exception =
        assertThrows(CustomException.class, () -> executor.updateAgreements(7L, request));

    assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    verify(termRepository, never()).findById(11L);
  }

  @Test
  void rejectsAnInactiveTerm() {
    UserRepository userRepository = mock(UserRepository.class);
    TermRepository termRepository = mock(TermRepository.class);
    UserTermAgreementRepository agreementRepository = mock(UserTermAgreementRepository.class);
    UserTermAgreementUpdateExecutor executor =
        new UserTermAgreementUpdateExecutor(userRepository, termRepository, agreementRepository);
    TermAgreementUpdateRequest request = mock(TermAgreementUpdateRequest.class);
    TermAgreementUpdateRequest.TermAgreementDto agreementDto =
        mock(TermAgreementUpdateRequest.TermAgreementDto.class);
    User user = mock(User.class);
    Term term = mock(Term.class);

    when(request.getAgreements()).thenReturn(List.of(agreementDto));
    when(agreementDto.getTermId()).thenReturn(11L);
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(termRepository.findById(11L)).thenReturn(Optional.of(term));
    when(term.isActive()).thenReturn(false);

    CustomException exception =
        assertThrows(CustomException.class, () -> executor.updateAgreements(7L, request));

    assertEquals(ErrorCode.INACTIVE_TERM, exception.getErrorCode());
    verify(agreementRepository, never()).save(any());
  }
}
