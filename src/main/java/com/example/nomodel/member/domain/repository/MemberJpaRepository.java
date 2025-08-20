package com.example.nomodel.member.domain.repository;

import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(Email email);
}
