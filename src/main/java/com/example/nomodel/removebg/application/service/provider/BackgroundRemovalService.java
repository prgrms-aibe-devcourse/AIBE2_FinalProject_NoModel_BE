package com.example.nomodel.removebg.application.service.provider;

import java.util.Map;

public interface BackgroundRemovalService {
    Long removeBackground(Long originalFileId, Map<String, Object> opts) throws Exception;
}