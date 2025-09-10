package com.example.nomodel.model.domain.model;

import com.example.nomodel._core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
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
  
  @Column(name = "member_id", nullable = false)
  private Long memberId;
  
  @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
  private String prompt;
  
  @Column(name = "ad_result_name", length = 200)
  private String adResultName;
  
  @Column(name = "member_rating")
  private Double memberRating;
  
  @Column(name = "result_image_url", length = 500)
  private String resultImageUrl;
  
  public static AdResult create(Long modelId, Long memberId, String prompt, String adResultName) {
    return AdResult.builder()
      .modelId(modelId)
      .memberId(memberId)
      .prompt(prompt)
      .adResultName(adResultName)
      .build();
  }
  
  public void updateRating(Double rating) {
    this.memberRating = rating;
  }
  
  public void updateResultImageUrl(String imageUrl) {
    this.resultImageUrl = imageUrl;
  }
  
  public void updateAdResultName(String name) {
    this.adResultName = name;
  }
}
