package com.barrierfree.bf.user.dto;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.global.enums.Role;
import java.util.List;

public record UserPreferenceResponse(
    Long userId,
    String nickname,
    Role role,
    List<MobilityType> mobilities,
    List<FacilityType> facilities) {}
