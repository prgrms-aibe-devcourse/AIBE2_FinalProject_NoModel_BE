package com.example.nomodel.model.command.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.command.application.dto.request.ModelUpdateRequest;
import com.example.nomodel.model.command.application.service.MyModelCommandService;
import com.example.nomodel.model.command.domain.event.ModelUpdateEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me/models")
@RequiredArgsConstructor
public class MyModelCommandController {

    private final MyModelCommandService myModelCommandService;

    @PatchMapping
    public ResponseEntity<?> updateMyModel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ModelUpdateRequest request
    ) {
        ModelUpdateEvent event = myModelCommandService.updateModel(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiUtils.success(event));
    }
}
