package com.barrierfree.bf.user.dto;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저 선호 필터 단독 수정 API 요청 데이터를 담는 DTO */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceUpdateRequest {

    // 둘 중 하나만 요청으로 들어올 수 있으므로 @NotNull을 제거하여 유연성 확보
    private List<MobilityType> mobilities;

    private List<FacilityType> facilities;
}
