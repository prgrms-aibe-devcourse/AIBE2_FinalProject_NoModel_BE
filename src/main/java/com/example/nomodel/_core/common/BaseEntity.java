package com.example.nomodel._core.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity extends BaseTimeEntity {

    @CreatedBy
    @Column(updatable = false)
    private String createdBy; // 생성자
    @LastModifiedBy
    private String modifiedBy; // 수정자
}
