package com.example.reclaimbackend.repository;

import com.example.reclaimbackend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Users in MongoDB.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by email.
     *
     * @param email the email
     * @return the user, or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Whether this email already has an account.
     *
     * @param email the email
     * @return {@code true} if taken
     */
    boolean existsByEmail(String email);
}
