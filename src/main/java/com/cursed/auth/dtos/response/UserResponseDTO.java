package com.cursed.auth.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {
    String email;
    String username;
    String displayName;
    String firstName;
    String lastName;
    String middleName;
    String role;
    boolean verified;
    boolean isActive;
}
