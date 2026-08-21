package com.barrierfree.bf.taxi.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 생성한 장애인 콜택시 예약 시도 내역을 저장하는 Entity
 */
@Entity
@Table(name = "taxi_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxiReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예약을 시도한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 매칭된 센터
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxi_center_id", nullable = false)
    private TaxiCenter taxiCenter;

    @Column(nullable = false)
    private String departureAddress;

    @Column(nullable = false)
    private String destinationAddress;

    // LLM이 산출한 예상 요금 (문자열 형태 가능성 고려)
    @Column(length = 50)
    private String estimatedFare;

    // LLM이 자동완성한 기사/센터 전송용 문자 메시지
    @Column(columnDefinition = "TEXT")
    private String generatedMessage;

    // FE에서 입력받은 동적 메타데이터 (JSON 직렬화 형태로 저장)
    @Column(columnDefinition = "TEXT")
    private String userInputMetadata;

    @Builder
    public TaxiReservation(User user, TaxiCenter taxiCenter, String departureAddress, 
                           String destinationAddress, String estimatedFare, 
                           String generatedMessage, String userInputMetadata) {
        this.user = user;
        this.taxiCenter = taxiCenter;
        this.departureAddress = departureAddress;
        this.destinationAddress = destinationAddress;
        this.estimatedFare = estimatedFare;
        this.generatedMessage = generatedMessage;
        this.userInputMetadata = userInputMetadata;
    }
}