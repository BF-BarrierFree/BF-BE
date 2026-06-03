package com.barrierfree.bf.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 상속받는 엔티티들이 아래 필드들을 컬럼으로 인식하도록 합니다.
@EntityListeners(AuditingEntityListener.class) // Auditing 기능을 포함시킵니다.
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt; // 생성 일시

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정 일시
}