package com.barrierfree.bf.user.dto;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.enums.Role;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 유저 프로필 조회 API 응답을 위한 DTO
 */
@Getter
@Builder
public class UserProfileResponse {

    private String nickname;
    private String profileImageUrl;
    private Role role;
    private List<MobilityType> mobilities;
    private List<FacilityType> facilities;
}
