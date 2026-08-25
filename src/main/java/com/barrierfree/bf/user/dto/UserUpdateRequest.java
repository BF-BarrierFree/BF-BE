package com.barrierfree.bf.user.dto;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저 정보 수정 API 요청 데이터를 담는 DTO */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserUpdateRequest {

  @NotBlank(message = "닉네임을 입력해주세요.")
  @Size(max = 15, message = "닉네임은 15자 이내로 입력해주세요.")
  private String nickname;

  @NotNull(message = "이동 유형을 선택해주세요. (해당 없으면 빈 리스트 전달)")
  private List<MobilityType> mobilities;

  @NotNull(message = "필요 시설을 선택해주세요. (해당 없으면 빈 리스트 전달)")
  private List<FacilityType> facilities;
}
