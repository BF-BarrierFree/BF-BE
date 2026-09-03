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
        // 동일한 제목의 약관이 있으면 버전을 증가시키고, 없으면 1로 생성하는 로직 등이 추가될 수 있습니다.
        // 현재는 단순 생성으로 구현합니다. (추후 버전 관리 고도화 가능)

        Term term = Term.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .isRequired(request.getIsRequired())
            .version(1) // 기본 버전 1
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
