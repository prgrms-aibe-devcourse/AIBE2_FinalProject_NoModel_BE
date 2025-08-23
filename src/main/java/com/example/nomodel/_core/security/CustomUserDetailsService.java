package com.example.nomodel._core.security;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.Status;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Member member = memberJpaRepository.findByEmail(Email.of(email))
                .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.getStatus().equals(Status.ACTIVE)) {
            throw new ApplicationException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        return new CustomUserDetails(
                member.getId(),
                member.getEmail().getValue(),
                member.getPassword().getValue(),
                List.of(new SimpleGrantedAuthority(member.getRole().getKey()))
        );
    }
}