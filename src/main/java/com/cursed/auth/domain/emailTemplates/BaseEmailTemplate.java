package com.cursed.auth.domain.emailTemplates;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEmailTemplate {
    String ssoBasePath;
    String title;
    String receiverName;
    int tmYear;
}
