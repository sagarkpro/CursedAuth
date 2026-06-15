package com.cursed.auth.domain.emailTemplates;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ForgotPasswordEmailTemplate extends BaseEmailTemplate {
    String token;
    String email;
}
