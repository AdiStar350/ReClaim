package com.example.reclaimbackend.repository;

import com.example.reclaimbackend.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * MongoDB repository for {@link PasswordResetToken} documents.
 */
public interface PasswordResetTokenRepository
        extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByEmail(String email);

    void deleteByEmail(String email);
}
