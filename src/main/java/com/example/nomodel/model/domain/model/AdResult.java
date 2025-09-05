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
@Table(name = "ad_result_tb")
public class AdResult extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ad_result_id")
  private Long id;
  
  @ManyToOne
  @JoinColumn(name = "model_id", nullable = false)
  private AIModel aiModel;
  
  @Column(name = "prompt", nullable = false)
  private String prompt;
}
