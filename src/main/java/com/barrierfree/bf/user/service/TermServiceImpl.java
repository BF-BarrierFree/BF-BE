package com.barrierfree.bf.user.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.user.dto.TermCreateRequest;
import com.barrierfree.bf.user.dto.TermResponse;
import com.barrierfree.bf.user.entity.Term;
import com.barrierfree.bf.user.repository.TermRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;

    @Override
    @Transactional
    public TermResponse createTerm(TermCreateRequest request) {
        termRepository.lockTermKey(request.getTermKey());

        Term latestRevision =
            termRepository.findFirstByTermKeyOrderByVersionDesc(request.getTermKey()).orElse(null);
        int nextVersion = latestRevision == null ? 1 : latestRevision.getVersion() + 1;
        termRepository
            .findByTermKeyAndIsActiveTrue(request.getTermKey())
            .ifPresent(Term::deactivate);

        Term term = Term.builder()
            .termKey(request.getTermKey())
            .title(request.getTitle())
            .content(request.getContent())
            .isRequired(request.getIsRequired())
            .version(nextVersion)
            .isActive(true)
            .build();

        Term savedTerm = termRepository.save(term);
        return TermResponse.from(savedTerm);
    }

    @Override
    public List<TermResponse> getActiveTerms() {
        return termRepository.findAllByIsActiveTrue().stream()
            .map(TermResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    public TermResponse getTerm(Long termId) {
        Term term = termRepository.findById(termId)
            .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND));
        return TermResponse.from(term);
    }
}
