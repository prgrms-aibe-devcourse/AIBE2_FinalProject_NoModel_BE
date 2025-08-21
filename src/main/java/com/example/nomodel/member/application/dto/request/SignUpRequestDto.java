package com.example.nomodel.member.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpDto(
        @NotBlank(message = "사용자명은 필수입니다")
        @Size(min = 2, max = 20, message = "사용자명은 2자 이상 20자 이하여야 합니다")
        String username,
        
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,
        
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하여야 합니다")
        String password
) {
}
