package com.barrierfree.bf.user.dto;

import com.barrierfree.bf.user.entity.Term;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermResponse {

    private final Long id;
    private final String termKey;
    private final String title;
    private final String content;
    private final boolean isRequired;
    private final Integer version;

    public static TermResponse from(Term term) {
        return TermResponse.builder()
            .id(term.getId())
            .termKey(term.getTermKey())
            .title(term.getTitle())
            .content(term.getContent())
            .isRequired(term.isRequired())
            .version(term.getVersion())
            .build();
    }
}
