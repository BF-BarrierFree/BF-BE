package com.barrierfree.bf.user.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.barrierfree.bf.user.dto.TermAgreementUpdateRequest;
import com.barrierfree.bf.user.repository.TermRepository;
import com.barrierfree.bf.user.repository.UserRepository;
import com.barrierfree.bf.user.repository.UserTermAgreementRepository;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UserTermServiceImplTest {

  private UserTermAgreementUpdateExecutor agreementUpdateExecutor;
  private UserTermServiceImpl userTermService;
  private TermAgreementUpdateRequest request;

  @BeforeEach
  void setUp() {
    agreementUpdateExecutor = mock(UserTermAgreementUpdateExecutor.class);
    userTermService =
        new UserTermServiceImpl(
            mock(UserRepository.class),
            mock(TermRepository.class),
            mock(UserTermAgreementRepository.class),
            agreementUpdateExecutor);
    request = mock(TermAgreementUpdateRequest.class);
  }

  @Test
  void retriesInANewTransactionAfterConcurrentAgreementInsert() {
    DataIntegrityViolationException conflict =
        new DataIntegrityViolationException(
            "duplicate agreement",
            new SQLException("uk_user_term_agreements_user_term constraint violated"));
    doThrow(conflict).doNothing().when(agreementUpdateExecutor).updateAgreements(7L, request);

    userTermService.updateAgreements(7L, request);

    verify(agreementUpdateExecutor, times(2)).updateAgreements(7L, request);
  }

  @Test
  void doesNotRetryUnrelatedIntegrityViolation() {
    DataIntegrityViolationException conflict =
        new DataIntegrityViolationException("different constraint");
    doThrow(conflict).when(agreementUpdateExecutor).updateAgreements(7L, request);

    assertThrows(
        DataIntegrityViolationException.class,
        () -> userTermService.updateAgreements(7L, request));

    verify(agreementUpdateExecutor).updateAgreements(7L, request);
  }
}
