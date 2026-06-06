package com.example.reclaimbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contact details exposed only after a claim is approved.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactResponse {

    private String name;
    private String email;
    private String phoneNumber;

}
