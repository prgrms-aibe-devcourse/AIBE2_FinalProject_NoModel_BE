package com.example.nomodel.model.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SamplerType {

    EULER_A("Euler a", "euler_a"),
    EULER("Euler", "euler"),
    LMS("LMS", "lms"),
    HEUN("Heun", "heun"),
    DPM2("DPM2", "dpm2"),
    DPM2_A("DPM2 a", "dpm2_a"),
    DPM_PLUS_PLUS_2S_A("DPM++ 2S a", "dpm_plus_plus_2s_a"),
    DPM_PLUS_PLUS_2M("DPM++ 2M", "dpm_plus_plus_2m"),
    DPM_PLUS_PLUS_SDE("DPM++ SDE", "dpm_plus_plus_sde"),
    DPM_FAST("DPM fast", "dpm_fast"),
    DPM_ADAPTIVE("DPM adaptive", "dpm_adaptive"),
    LMS_KARRAS("LMS Karras", "lms_karras"),
    DPM2_KARRAS("DPM2 Karras", "dpm2_karras"),
    DPM2_A_KARRAS("DPM2 a Karras", "dpm2_a_karras"),
    DPM_PLUS_PLUS_2S_A_KARRAS("DPM++ 2S a Karras", "dpm_plus_plus_2s_a_karras"),
    DPM_PLUS_PLUS_2M_KARRAS("DPM++ 2M Karras", "dpm_plus_plus_2m_karras"),
    DPM_PLUS_PLUS_SDE_KARRAS("DPM++ SDE Karras", "dpm_plus_plus_sde_karras"),
    DDIM("DDIM", "ddim"),
    PLMS("PLMS", "plms");

    private final String displayName;
    private final String value;

    public boolean isKarrasScheduler() {
        return this.name().contains("KARRAS");
    }

    public boolean isDPMFamily() {
        return this.name().startsWith("DPM");
    }

    public boolean isEulerFamily() {
        return this.name().startsWith("EULER");
    }
}