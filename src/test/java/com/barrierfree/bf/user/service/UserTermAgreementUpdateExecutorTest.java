package com.barrierfree.bf.user.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    UserTermAgreementRepository agreementRepository =
        mock(UserTermAgreementRepository.class);
    UserTermAgreementUpdateExecutor executor =
        new UserTermAgreementUpdateExecutor(
            userRepository, termRepository, agreementRepository);
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
    when(term.getId()).thenReturn(11L);
    when(agreementRepository.findByUserIdAndTermId(7L, 11L))
        .thenReturn(Optional.of(existingAgreement));

    executor.updateAgreements(7L, request);

    verify(existingAgreement).updateAgreement(false);
    verify(agreementRepository).save(existingAgreement);
  }
}
