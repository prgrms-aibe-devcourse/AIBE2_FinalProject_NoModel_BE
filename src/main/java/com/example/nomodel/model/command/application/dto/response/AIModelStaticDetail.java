package com.example.nomodel.model.command.application.dto.response;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.model.command.application.dto.AIModelDetailResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIModelStaticDetail {
    private Long modelId;
    private String modelName;
    private String description;
    private String ownType;
    private String ownerName;
    private Long ownerId;
    private BigDecimal price;
    private boolean isPublic;
    private List<AIModelDetailResponse.FileInfo> files;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
