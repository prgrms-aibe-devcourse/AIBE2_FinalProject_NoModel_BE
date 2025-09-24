package com.example.nomodel.model.command.domain.service;

import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.ModelStatistics;
import com.example.nomodel.model.command.domain.repository.ModelStatisticsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelStatisticsService {

    private final ModelStatisticsJpaRepository modelStatisticsRepository;

    @Transactional
    public void createInitialStatistics(AIModel model) {
        ModelStatistics statistics = ModelStatistics.createInitialStatistics(model);
        modelStatisticsRepository.save(statistics);
    }
}
