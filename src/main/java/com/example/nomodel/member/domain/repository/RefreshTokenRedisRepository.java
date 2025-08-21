package com.example.nomodel.member.domain.repository;

import com.example.nomodel.member.domain.model.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {

    RefreshToken findByRefreshToken(String refreshToken);
    
    void deleteByRefreshToken(String refreshToken);
}
