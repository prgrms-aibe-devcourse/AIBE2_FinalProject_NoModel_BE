package com.example.nomodel.model.domain.model;

import com.example.nomodel._core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
  name = "ad_result_tb",
  indexes = {
    @Index(name = "idx_ad_result_created_at", columnList = "created_at"),
    @Index(name = "idx_ad_result_model_id", columnList = "model_id"),
    @Index(name = "idx_ad_result_member_id", columnList = "member_id")
  }
)
public class AdResult extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ad_result_id")
  private Long id;

  @Column(name = "model_id", nullable = false)
  private Long modelId;
  
  @Column(name = "model_name", nullable = false, length = 100)
  private String modelName;
  
  @Column(name = "member_id", nullable = false)
  private Long memberId;
  
  @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
  private String prompt;
  
  public static AdResult create(Long modelId, String modelName, Long memberId, String prompt) {
    return AdResult.builder()
      .modelId(modelId)
      .modelName(modelName)
      .memberId(memberId)
      .prompt(prompt)
      .build();
  }
}
