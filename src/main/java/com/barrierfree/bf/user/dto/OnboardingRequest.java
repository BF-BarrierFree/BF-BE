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

/** 온보딩 API 요청 데이터를 담는 DTO 프론트엔드에서 전달받는 유저의 추가 정보(닉네임, 다중 선택 항목, 약관 동의 내역)를 검증합니다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingRequest {

  @NotBlank(message = "닉네임을 입력해주세요.")
  @Size(max = 15, message = "닉네임은 15자 이내로 입력해주세요.")
  private String nickname;

  @NotNull(message = "이동 유형을 선택해주세요. (해당 없으면 빈 리스트 전달)")
  private List<MobilityType> mobilities;

  @NotNull(message = "필요 시설을 선택해주세요. (해당 없으면 빈 리스트 전달)")
  private List<FacilityType> facilities;

  @NotNull(message = "약관 동의 정보가 누락되었습니다.")
  private List<Long> agreedTermIds; // 유저가 동의(체크)한 약관의 ID 목록
}
