package com.example.nomodel.model.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "updateType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ModelUpdateRequest.PriceUpdate.class, name = "PRICE"),
        @JsonSubTypes.Type(value = ModelUpdateRequest.VisibilityUpdate.class, name = "VISIBILITY")
})
public sealed interface ModelUpdateRequest permits ModelUpdateRequest.PriceUpdate, ModelUpdateRequest.VisibilityUpdate {

    @NotNull Long modelId();

    ModelUpdateType updateType();

    record PriceUpdate(
            @NotNull Long modelId,
            @PositiveOrZero BigDecimal newPrice
    ) implements ModelUpdateRequest {
        @Override
        public ModelUpdateType updateType() {
            return ModelUpdateType.PRICE;
        }
    }

    record VisibilityUpdate(
            @NotNull Long modelId,
            @NotNull Boolean isPublic
    ) implements ModelUpdateRequest {
        @Override
        public ModelUpdateType updateType() {
            return ModelUpdateType.VISIBILITY;
        }
    }
}
