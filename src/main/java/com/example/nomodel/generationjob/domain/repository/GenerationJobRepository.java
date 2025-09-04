package com.example.nomodel.generationjob.domain.repository;

import com.example.nomodel.generationjob.domain.model.GenerationJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {}
