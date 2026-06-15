package com.cursed.auth.clients;

import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.cursed.auth.logging.CursedLogger;
import com.cursed.auth.constants.EmailTemplatePaths;
import com.cursed.auth.domain.SendForgotPasswordEmail;
import com.cursed.auth.domain.SendOTPVerificationMail;
import com.cursed.auth.domain.emailTemplates.BaseEmailTemplate;
import com.cursed.auth.domain.emailTemplates.EmailRecipient;
import com.cursed.auth.domain.emailTemplates.EmailRequest;
import com.cursed.auth.domain.emailTemplates.EmailSender;
import com.cursed.auth.domain.emailTemplates.ForgotPasswordEmailTemplate;
import com.cursed.auth.domain.emailTemplates.SendEmailRequest;
import com.cursed.auth.domain.emailTemplates.VerifyOtpEmailTemplate;

import freemarker.template.Configuration;
import freemarker.template.Template;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailClient {

	private final String BREVO_URL;
	private final String API_KEY;
	private final RestClient restClient;
	private final Configuration freeMarkerConfig;
	private final ObjectMapper mapper;
	private final String SSO_BASEPATH;
	private final String SENDER_NAME;
	private final String SENDER_EMAIL;

	public BrevoEmailClient(@Value("${brevo-config.BREVO_URL}") String brevoUrl,
			@Value("${brevo-config.BREVO_API_KEY}") String apiKey,
			@Value("${sso.login.base-path}") String ssoBasePath,
			@Value("${brevo-config.sender.name}") String senderName,
			@Value("${brevo-config.sender.email}") String senderEmail,
			Configuration freeMarkerConfig,
			ObjectMapper mapper) {
		this.BREVO_URL = brevoUrl;
		this.API_KEY = apiKey;
		this.SSO_BASEPATH = ssoBasePath;
		this.SENDER_NAME = senderName;
		this.SENDER_EMAIL = senderEmail;
		this.freeMarkerConfig = freeMarkerConfig;
		this.mapper = mapper;
		this.restClient = RestClient.builder()
				.baseUrl(BREVO_URL)
				.defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("api-key", API_KEY)
				.build();
	}

	public JsonNode sendEmail(EmailRequest request) throws Exception {
		SendEmailRequest emailRequest = SendEmailRequest.builder()
				.sender(EmailSender.builder().name(SENDER_NAME).email(SENDER_EMAIL).build())
				.to(List.of(
						EmailRecipient.builder()
								.email(request.getRecipientEmail())
								.name(request.getRecipientName())
								.build()))
				.subject(request.getSubject())
				.htmlContent(request.getHtmlContent())
				.build();

		Map<String, Object> body = mapper.convertValue(emailRequest, new TypeReference<Map<String, Object>>() {
		});

		JsonNode response = restClient.post()
				.body(body)
				.retrieve()
				.body(JsonNode.class);

		CursedLogger.info("SendEmail API result:" + response.toPrettyString());
		return response;
	}

	public JsonNode sendForgotPassword(SendForgotPasswordEmail request) throws Exception {
		String htmlContent = generateEmailTemplate(ForgotPasswordEmailTemplate.builder()
				.title("Email Verification OTP")
				.token(request.getToken())
				.email(request.getEmail())
				.receiverName(request.getReceiverName())
				.ssoBasePath(SSO_BASEPATH)
				.tmYear(java.time.Year.now().getValue())
				.build(), EmailTemplatePaths.FORGOT_PASSWORD_MAIL);

		return sendEmail(EmailRequest.builder()
				.recipientEmail(request.getReceiverEmail())
				.recipientName(request.getReceiverName())
				.subject("Cursed Auth - Password Recovery")
				.htmlContent(htmlContent)
				.build());
	}

	public JsonNode sendEmailVerificationOTP(SendOTPVerificationMail request) throws Exception {
		String htmlContent = generateEmailTemplate(VerifyOtpEmailTemplate.builder()
				.title("Email Verification OTP")
				.otp(request.getOTP())
				.receiverName(request.getReceiverName())
				.ssoBasePath(SSO_BASEPATH)
				.tmYear(java.time.Year.now().getValue())
				.build(), EmailTemplatePaths.VERIFY_OTP_MAIL);

		return sendEmail(EmailRequest.builder()
				.recipientEmail(request.getReceiverEmail())
				.recipientName(request.getReceiverName())
				.subject("Cursed Auth - Email Verification OTP")
				.htmlContent(htmlContent)
				.build());
	}

	private String generateEmailTemplate(BaseEmailTemplate req, String emailPath) throws Exception {
		Template template = freeMarkerConfig.getTemplate(emailPath);
		var templateVars = mapper.convertValue(req, new TypeReference<Map<String, Object>>() {
		});
		return FreeMarkerTemplateUtils.processTemplateIntoString(template, templateVars);
	}
}
