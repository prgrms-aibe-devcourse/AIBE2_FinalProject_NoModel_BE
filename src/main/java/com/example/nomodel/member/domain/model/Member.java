package com.example.nomodel.member.domain.model;

import com.example.nomodel._core.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_tb")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id; // 회원 ID

    @Column(name = "username", nullable = false)
    private String username; // 사용자 이름
    @Embedded
    private Email email; // 이메일
    @Embedded
    private Password password; // 비밀번호

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Builder
    private Member(String username, Email email, Password password, Role role, Status status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    public static Member createMember(String username, Email email, Password password) {
        return Member.builder()
                .username(username)
                .email(email)
                .password(password)
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();
    }

    public void deactivate() {
        this.status = Status.SUSPENDED;
    }

    /**
     * 비밀번호 검증
     * @param rawPassword 입력된 비밀번호
     * @param passwordEncoder 비밀번호 인코더
     * @return 비밀번호 일치 여부
     */
    public boolean validatePassword(String rawPassword, PasswordEncoder passwordEncoder) {
        return this.password.matches(rawPassword, passwordEncoder);
    }

    /**
     * 회원 상태가 활성화되어 있는지 확인
     * @return 활성화 여부
     */
    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }
}