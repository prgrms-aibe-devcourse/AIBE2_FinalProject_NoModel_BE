package com.example.nomodel.model.application.controller;


import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.response.AdminAIModelResponseDto;
import com.example.nomodel.model.application.service.AdminAIModelService;
import com.example.nomodel.model.domain.model.AIModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
public class AdminAIModelController {
  
  private final AdminAIModelService adminAIModelService;
  
  @GetMapping
  public ResponseEntity<?> getAdminModels(
          @RequestParam(name = "page", defaultValue = "0") int page,
          @RequestParam(name = "size", defaultValue = "10") int size,
          @RequestParam(name="keyword", required = false) String keyword
  ) {
    Page<List<AdminAIModelResponseDto>> result = adminAIModelService.getAdminModels(page, size, keyword);
    return ResponseEntity.ok(ApiUtils.success(result));
  }
  
  @PatchMapping("/price/{modelId}")
  public ResponseEntity<?> changePrice(
          @PathVariable(name = "modelId")String modelId,
          @RequestBody BigDecimal price) {
    AIModel model = adminAIModelService.changePrice(modelId, price);
    return ResponseEntity.ok(ApiUtils.success(model));
  }
  
  @PatchMapping("/isPublic/{modelId}")
  public ResponseEntity<?> changeIsPublic(
          @PathVariable(name = "modelId")String modelId,
          @RequestBody boolean isPublic) {
    AIModel model = adminAIModelService.changeIsPublic(modelId, isPublic);
    return ResponseEntity.ok(ApiUtils.success(model));
  }
}
