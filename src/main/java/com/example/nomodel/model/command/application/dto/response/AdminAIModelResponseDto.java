package com.example.nomodel.model.application.dto.response;

import com.example.nomodel.model.domain.model.OwnType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
public class AdminAIModelResponseDto {
  public String id;
  public Long modelId;
  public String modelName;
  public String prompt;
  public OwnType ownType;
  public BigDecimal price;
  public boolean isPublic;
  public String ownerName;
  public Long usageCount;
  public Long viewCount;
  public LocalDateTime createdAt;
  
  public AdminAIModelResponseDto(String id, Long modelId, String modelName, String prompt,
                                 OwnType ownType, BigDecimal price, boolean isPublic,
                                 String ownerName,
                                 Long usageCount,
                                 Long viewCount,
                                 LocalDateTime createdAt
  ) {
    this.id = id;
    this.modelId = modelId;
    this.modelName = modelName;
    this.prompt = prompt;
    this.ownType = ownType;
    this.price = price;
    this.isPublic = isPublic;
    this.ownerName = ownerName;
    this.usageCount = usageCount;
    this.viewCount = viewCount;
    this.createdAt = createdAt;
  }
}
