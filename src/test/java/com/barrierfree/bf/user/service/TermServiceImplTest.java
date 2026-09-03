package com.barrierfree.bf.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.user.dto.TermCreateRequest;
import com.barrierfree.bf.user.dto.TermResponse;
import com.barrierfree.bf.user.entity.Term;
import com.barrierfree.bf.user.repository.TermRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TermServiceImplTest {

  private TermRepository termRepository;
  private TermServiceImpl termService;
  private TermCreateRequest request;

  @BeforeEach
  void setUp() {
    termRepository = mock(TermRepository.class);
    termService = new TermServiceImpl(termRepository);
    request = mock(TermCreateRequest.class);

    when(request.getTermKey()).thenReturn("PRIVACY_POLICY");
    when(request.getTitle()).thenReturn("Privacy policy");
    when(request.getContent()).thenReturn("Policy content");
    when(request.getIsRequired()).thenReturn(true);
    when(termRepository.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void createsFirstRevisionAtVersionOne() {
    when(termRepository.findFirstByTermKeyOrderByVersionDesc("PRIVACY_POLICY"))
        .thenReturn(Optional.empty());

    TermResponse response = termService.createTerm(request);

    verify(termRepository).lockTermKey("PRIVACY_POLICY");
    assertEquals("PRIVACY_POLICY", response.getTermKey());
    assertEquals(1, response.getVersion());
  }

  @Test
  void deactivatesCurrentRevisionAndIncrementsVersion() {
    Term currentRevision =
        Term.builder()
            .termKey("PRIVACY_POLICY")
            .title("Old privacy policy")
            .content("Old content")
            .isRequired(true)
            .isActive(true)
            .version(2)
            .build();
    when(termRepository.findFirstByTermKeyOrderByVersionDesc("PRIVACY_POLICY"))
        .thenReturn(Optional.of(currentRevision));
    when(termRepository.findByTermKeyAndIsActiveTrue("PRIVACY_POLICY"))
        .thenReturn(Optional.of(currentRevision));

    TermResponse response = termService.createTerm(request);

    ArgumentCaptor<Term> savedTerm = ArgumentCaptor.forClass(Term.class);
    verify(termRepository).save(savedTerm.capture());
    assertFalse(currentRevision.isActive());
    assertTrue(savedTerm.getValue().isActive());
    assertEquals(3, response.getVersion());
  }
}
