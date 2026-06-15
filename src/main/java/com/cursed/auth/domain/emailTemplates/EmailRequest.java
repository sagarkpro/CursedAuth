package com.cursed.auth.domain.emailTemplates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequest {
	private String recipientEmail;
	private String recipientName;
	private String subject;
	private String htmlContent;
}
