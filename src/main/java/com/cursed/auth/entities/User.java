package com.cursed.auth.entities;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.cursed.auth.entities.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder

@Document(collection = "users")
public class User extends BaseEntity implements UserDetails {
    @Indexed(unique = true)
    String email;
    @Indexed(unique = true)
    String username;
    String password;
    String firstName;
    String lastName;
    String middleName;
    Role role;
    boolean verified;

    @Builder.Default
    boolean isActive = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.getRole().toString()));
    }

    @Override
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder(this.firstName);
        if (!StringUtils.isBlank(this.middleName)) {
            sb.append(" ").append(this.middleName);
        }
        if (!StringUtils.isBlank(this.lastName)) {
            sb.append(" ").append(this.lastName);
        }
        return sb.toString();
    }
}