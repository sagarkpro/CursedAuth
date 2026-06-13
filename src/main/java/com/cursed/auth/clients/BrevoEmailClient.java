package com.cursed.auth.clients;

import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.cursed.auth.domain.EmailRecipient;
import com.cursed.auth.domain.EmailSender;
import com.cursed.auth.domain.SendEmailRequest;
import com.cursed.auth.domain.VerifyOtpEmailTemplate;
import com.cursed.auth.logging.CursedLogger;
import com.cursed.auth.records.SendOTPVerificationMail;

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

	public JsonNode sendEmailVerificationOTP(SendOTPVerificationMail request) throws Exception {
		String emailContent = generateOtpVerificationTemplate(VerifyOtpEmailTemplate.builder()
				.otp(request.OTP())
				.receiverName(request.receiverName())
				.ssoBasePath(SSO_BASEPATH)
				.tmYear(java.time.Year.now().getValue())
				.build());

		SendEmailRequest emailRequest = SendEmailRequest.builder()
				.sender(EmailSender.builder().name(SENDER_NAME).email(SENDER_EMAIL).build())
				.to(List.of(
						EmailRecipient.builder().email(request.receiverEmail()).name(request.receiverName()).build()))
				.subject("Cursed Auth - Email Verification OTP")
				.htmlContent(emailContent)
				.build();

		Map<String, Object> body = mapper.convertValue(emailRequest, new TypeReference<Map<String, Object>>() {
		});

		JsonNode response = restClient.post()
				.body(body)
				.retrieve()
				.body(JsonNode.class);

		CursedLogger.info("SendVerificationOTPEmail API result:" + response.toPrettyString());
		return response;
	}

	private String generateOtpVerificationTemplate(VerifyOtpEmailTemplate req) throws Exception {
		Template template = freeMarkerConfig.getTemplate("emails/verify-otp.html");
		var templateVars = mapper.convertValue(req, new TypeReference<Map<String, Object>>() {
		});
		return FreeMarkerTemplateUtils.processTemplateIntoString(template, templateVars);
	}
}
