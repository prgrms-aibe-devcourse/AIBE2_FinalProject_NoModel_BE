package com.example.nomodel.model.command.application.dto.request;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;

import java.util.Arrays;

public enum ModelUpdateType {
    PRICE,
    VISIBILITY;

    public static ModelUpdateType from(String value) {
        if (value == null) {
            throw new ApplicationException(ErrorCode.INVALID_ENUM_VALUE);
        }

        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_ENUM_VALUE));
    }
}
