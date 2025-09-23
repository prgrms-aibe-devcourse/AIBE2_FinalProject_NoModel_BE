package com.example.nomodel.member.domain.model;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Embeddable
public class Password {

    @Column(name = "password", nullable = false)
    private String value;

    protected Password() {}

    private Password(String value) {
        this.value = value;
    }

    public static Password encode(String value, PasswordEncoder encoder) {
        validatePassword(value);
        return new Password(encoder.encode(value));
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 4) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }
    }

    public boolean matches(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.value);
    }
}
