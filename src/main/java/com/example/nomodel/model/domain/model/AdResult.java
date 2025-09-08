package com.example.nomodel.model.domain.model;

import com.example.nomodel._core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
  name = "ad_result_tb",
  indexes = {
    @Index(name = "idx_ad_result_created_at", columnList = "created_at"),
    @Index(name = "idx_ad_result_model_id", columnList = "model_id")
  }
)
public class AdResult extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ad_result_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "model_id", nullable = false)
  private AIModel aiModel;
  
  @Column(name = "prompt", nullable = false)
  private String prompt;
}
