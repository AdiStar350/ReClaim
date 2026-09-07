package com.example.reclaimbackend.repository;

import com.example.reclaimbackend.model.PendingRegistration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Pending signups in MongoDB.
 */
public interface PendingRegistrationRepository
        extends MongoRepository<PendingRegistration, String> {

    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);
}
