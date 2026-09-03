package com.barrierfree.bf.user.dto;

import com.barrierfree.bf.user.entity.UserTermAgreement;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserTermAgreementResponse {

    private final Long agreementId;
    private final Long termId;
    private final String termTitle;
    private final boolean isRequired;
    private final boolean isAgreed;
    private final Integer agreedVersion;
    private final LocalDateTime agreedAt;

    public static UserTermAgreementResponse from(UserTermAgreement agreement) {
        return UserTermAgreementResponse.builder()
            .agreementId(agreement.getId())
            .termId(agreement.getTerm().getId())
            .termTitle(agreement.getTerm().getTitle())
            .isRequired(agreement.getTerm().isRequired())
            .isAgreed(agreement.isAgreed())
            .agreedVersion(agreement.getTerm().getVersion())
            .agreedAt(agreement.getUpdatedAt()) // 최근 상태가 변경된 시간 반환
            .build();
    }
}
