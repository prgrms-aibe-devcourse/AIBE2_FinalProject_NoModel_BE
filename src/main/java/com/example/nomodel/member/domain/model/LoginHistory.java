package com.example.nomodel.member.domain.model;

import com.example.nomodel._core.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "login_history_tb",
        indexes = {
                @Index(name = "idx_login_history_member_id", columnList = "member_id"),
                @Index(name = "idx_login_history_created_at", columnList = "created_at")
        }
)
public class LoginHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "hashed_ip", length = 64, nullable = false)
    private String hashedIp;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_status", nullable = false, length = 20)
    private LoginStatus loginStatus;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Builder
    private LoginHistory(Member member, String hashedIp, LoginStatus loginStatus, String failureReason) {
        this.member = member;
        this.hashedIp = hashedIp;
        this.loginStatus = loginStatus;
        this.failureReason = failureReason;
    }

    public static LoginHistory createSuccessHistory(Member member, String hashedIp) {
        return LoginHistory.builder()
                .member(member)
                .hashedIp(hashedIp)
                .loginStatus(LoginStatus.SUCCESS)
                .build();
    }

    public static LoginHistory createFailureHistory(String hashedIp, String failureReason) {
        return LoginHistory.builder()
                .hashedIp(hashedIp)
                .loginStatus(LoginStatus.FAILURE)
                .failureReason(failureReason)
                .build();
    }
    
    public void setMember(Member member) {
        this.member = member;
    }
}