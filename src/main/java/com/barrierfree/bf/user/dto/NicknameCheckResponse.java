package com.barrierfree.bf.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NicknameCheckResponse {

    // true면 사용 가능(중복 안됨), false면 사용 불가(중복됨)
    private boolean isAvailable;
}
