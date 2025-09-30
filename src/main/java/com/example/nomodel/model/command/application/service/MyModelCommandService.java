package com.example.nomodel.model.command.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.command.application.dto.request.ModelUpdateRequest;
import com.example.nomodel.model.command.domain.event.ModelUpdateEvent;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import lombok.RequiredArgsConstructor;
import com.example.nomodel.model.command.domain.event.ModelUpdateEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MyModelCommandService {

    private final AIModelJpaRepository aiModelRepository;
    private final ModelUpdateEventPublisher eventPublisher;

    @Transactional
    public ModelUpdateEvent updateModel(Long memberId, ModelUpdateRequest request) {
        AIModel model = aiModelRepository.findByIdAndOwnerId(request.modelId(), memberId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AI_MODEL_NOT_FOUND));

        ModelUpdateEvent event = switch (request) {
            case ModelUpdateRequest.PriceUpdate priceUpdate -> handlePriceUpdate(model, priceUpdate);
            case ModelUpdateRequest.VisibilityUpdate visibilityUpdate -> handleVisibilityUpdate(model, visibilityUpdate);
        };

        eventPublisher.publishAfterCommit(event);
        return event;
    }

    private ModelUpdateEvent handlePriceUpdate(AIModel model, ModelUpdateRequest.PriceUpdate request) {
        BigDecimal oldPrice = model.getPrice();
        BigDecimal updatedPrice = request.newPrice();

        if (updatedPrice != null && updatedPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        model.updatePrice(updatedPrice);
        aiModelRepository.save(model);

        return ModelUpdateEvent.priceChange(
                model.getId(),
                oldPrice,
                updatedPrice
        );
    }

    private ModelUpdateEvent handleVisibilityUpdate(AIModel model, ModelUpdateRequest.VisibilityUpdate request) {
        boolean oldVisibility = model.isPublic();
        boolean updatedVisibility = request.isPublic();

        model.updateVisibility(updatedVisibility);
        aiModelRepository.save(model);

        return ModelUpdateEvent.visibilityChange(
                model.getId(),
                oldVisibility,
                updatedVisibility
        );
    }
}
