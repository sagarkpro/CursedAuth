package com.cursed.auth.dtos;

import org.hibernate.validator.constraints.Length;

import com.cursed.auth.entities.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterDTO {
    @NotBlank
    @Email
    String email;
    @NotBlank
    @Length(min = 2, max = 100)
    String username;
    @NotBlank
    @Length(min = 2, max = 100)
    String password;
    @NotBlank
    @Length(min = 2, max = 100)
    String firstName;
    @Length(min = 2, max = 100)
    String lastName;
    @Length(min = 2, max = 100)
    String middleName;
    @NotNull
    Role role;
}
