package com.barrierfree.bf.review.dto;

import com.barrierfree.bf.global.enums.FacilityType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 장소 상세 조회 시 RDBMS 통계(COUNT) 결과를 받아오기 위한 DTO
 */
@Getter
@AllArgsConstructor
public class FacilityCountDto {
    private FacilityType facilityType;
    private Long count;
}