package com.barrierfree.bf.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermAgreementUpdateRequest {

  @NotEmpty(message = "변경할 약관 목록이 비어있습니다.")
  @Valid
  private List<@NotNull(message = "약관 항목은 null일 수 없습니다.") TermAgreementDto> agreements;

  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class TermAgreementDto {

    @NotNull(message = "약관 ID는 필수입니다.")
    @Positive(message = "약관 ID는 양수여야 합니다.")
    private Long termId;

    @NotNull(message = "동의 상태값은 필수입니다.")
    private Boolean isAgreed;
  }
}
