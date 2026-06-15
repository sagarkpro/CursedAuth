package com.cursed.auth.domain.emailTemplates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendEmailRequest {
    private EmailSender sender;
    private List<EmailRecipient> to;
    private String subject;
    private String htmlContent;
}
