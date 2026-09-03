package com.barrierfree.bf.user.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.user.dto.TermAgreementUpdateRequest;
import com.barrierfree.bf.user.entity.Term;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.entity.UserTermAgreement;
import com.barrierfree.bf.user.repository.TermRepository;
import com.barrierfree.bf.user.repository.UserRepository;
import com.barrierfree.bf.user.repository.UserTermAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserTermAgreementUpdateExecutor {

  private final UserRepository userRepository;
  private final TermRepository termRepository;
  private final UserTermAgreementRepository userTermAgreementRepository;

  @Transactional
  public void updateAgreements(Long userId, TermAgreementUpdateRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    for (TermAgreementUpdateRequest.TermAgreementDto dto : request.getAgreements()) {
      Term term =
          termRepository
              .findById(dto.getTermId())
              .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND));

      if (term.isRequired() && !dto.getIsAgreed()) {
        throw new CustomException(ErrorCode.REQUIRED_TERM_CANCELLATION_NOT_ALLOWED);
      }

      UserTermAgreement agreement =
          userTermAgreementRepository
              .findByUserIdAndTermId(userId, term.getId())
              .orElseGet(
                  () ->
                      UserTermAgreement.builder()
                          .user(user)
                          .term(term)
                          .isAgreed(dto.getIsAgreed())
                          .build());
      agreement.updateAgreement(dto.getIsAgreed());
      userTermAgreementRepository.save(agreement);
    }
  }
}
