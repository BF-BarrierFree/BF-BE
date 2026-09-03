package com.barrierfree.bf.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermCreateRequest {

    @NotBlank(message = "약관 제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "약관 내용을 입력해주세요.")
    private String content;

    @NotNull(message = "필수 약관 여부를 지정해주세요.")
    private Boolean isRequired;
}
