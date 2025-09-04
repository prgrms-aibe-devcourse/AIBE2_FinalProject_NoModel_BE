package com.example.nomodel.removebg.application.service.provider;

import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "BG_PROVIDER", havingValue = "dummy", matchIfMissing = true)
@RequiredArgsConstructor
public class DummyBackgroundRemovalService implements BackgroundRemovalService {

    private final FileService fileService;

    @Override
    public Long removeBackground(Long originalFileId, Map<String, Object> opts) throws Exception {

        byte[] original = fileService.loadAsBytes(originalFileId);
        return fileService.saveBytes(
                original,
                "image/png",
                RelationType.AD,
                originalFileId,
                FileType.PREVIEW
        );
    }
}
