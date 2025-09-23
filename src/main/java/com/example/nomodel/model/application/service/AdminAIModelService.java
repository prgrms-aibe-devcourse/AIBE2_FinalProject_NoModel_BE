package com.example.nomodel.model.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.application.dto.response.AdminAIModelResponseDto;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.OwnType;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAIModelService {
  
  private final AIModelJpaRepository aiModelJpaRepository;
  
  public Page<List<AdminAIModelResponseDto>> getAdminModels(int page, int size, String keyword) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    
    return aiModelJpaRepository.getAdminAIModel(keyword, pageable);
  
  }
  
  public AIModel changePrice(String modelId, BigDecimal price) {
    AIModel aiModel = aiModelJpaRepository.findById(Long.parseLong(modelId))
            .orElseThrow(() -> {
              throw new ApplicationException(ErrorCode.AI_MODEL_NOT_FOUND);
            });
    
    aiModel.setPrice(price);
    return aiModelJpaRepository.save(aiModel);
  }
  
  public AIModel changeIsPublic(String modelId, boolean isPublic) {
    AIModel aiModel = aiModelJpaRepository.findById(Long.parseLong(modelId))
            .orElseThrow(() -> {
              throw new ApplicationException(ErrorCode.AI_MODEL_NOT_FOUND);
            });
    
    aiModel.setPublic(isPublic);
    return aiModelJpaRepository.save(aiModel);
  }
}
