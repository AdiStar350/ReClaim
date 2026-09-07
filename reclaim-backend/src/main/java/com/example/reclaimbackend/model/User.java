package com.example.reclaimbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A user in the {@code users} collection. Email is unique.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String name;

    private String phoneNumber;

    /** Device token for push notifications. May be empty. */
    private String fcmToken;

    /** Profile picture URL. May be empty. */
    private String profilePictureUrl;

}
