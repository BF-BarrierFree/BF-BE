package com.barrierfree.bf.user.dto;

import lombok.Builder;
import lombok.Getter;

/** 프로필 이미지 등록·수정 결과를 반환하는 DTO입니다. */
@Getter
@Builder
public class UserProfileImageResponse {

  private String profileImageUrl;
}
