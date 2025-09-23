package com.example.nomodel._core.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
public class MetricsConfig {

    private static final String SPRING_BATCH_JOB_ACTIVE = "spring.batch.job.active";
    private static final String ORIGINAL_NAME_TAG = "spring.batch.job.name";
    private static final String NORMALIZED_NAME_TAG = "spring.batch.job.active.name";
    private static final String STATUS_TAG = "spring.batch.job.status";

    @Bean
    public MeterFilter springBatchJobActiveTagNormalizer() {
        return new MeterFilter() {
            @NonNull
            @Override
            public Meter.Id map(@NonNull Meter.Id id) {
                if (!SPRING_BATCH_JOB_ACTIVE.equals(id.getName())) {
                    return id;
                }

                List<Tag> normalizedTags = id.getTags().stream()
                        .map(tag -> {
                            if (ORIGINAL_NAME_TAG.equals(tag.getKey())) {
                                return Tag.of(NORMALIZED_NAME_TAG, tag.getValue());
                            }
                            if (STATUS_TAG.equals(tag.getKey())) {
                                return null; // drop conflicting status tag
                            }
                            return tag;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                return new Meter.Id(
                        id.getName(),
                        Tags.of(normalizedTags),
                        id.getBaseUnit(),
                        id.getDescription(),
                        id.getType()
                );
            }
        };
    }
}
