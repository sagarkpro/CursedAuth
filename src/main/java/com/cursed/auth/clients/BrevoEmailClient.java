package com.cursed.auth.clients;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.cursed.auth.logging.CursedLogger;
import com.cursed.auth.records.SendOTPVerificationMail;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailClient {

	private final String BREVO_URL;
	private final String API_KEY;
	private final RestClient restClient;

	public BrevoEmailClient(@Value("${brevo-config.BREVO_URL}") String brevoUrl,
			@Value("${brevo-config.BREVO_API_KEY}") String apiKey) {
		this.BREVO_URL = brevoUrl;
		this.API_KEY = apiKey;
		this.restClient = RestClient.builder()
				.baseUrl(BREVO_URL)
				.defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader("api-key", API_KEY)
				.build();
	}

	public JsonNode sendEmailVerificationOTP(SendOTPVerificationMail request) {

		Map<String, Object> body = Map.of(
				"sender", Map.of(
						"name", "Cursed Auth",
						"email", "chat@cursedshrine.co.in"),
				"to", List.of(
						Map.of(
								"email", request.receiverEmail(),
								"name", request.receiverName())),
				"subject", "Cursed Auth - Email Verification OTP",
				"htmlContent",
				String.format(
						"""
								<!DOCTYPE html>
								<html>

								<head>
								<meta charset="UTF-8">
								<title>Email Verification OTP</title>
								</head>

								<body style="
								                margin:0;
								                padding:0;
								                background-color:#0a0a0a;
								                color:#ededed;
								                font-family: Arial, Helvetica, sans-serif;
								                ">
								<table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#0a0a0a; padding:32px 0;">
								    <tr>
								    <td align="center">
								        <table width="100%%" cellpadding="0" cellspacing="0" style="
								                        max-width:520px;
								                        background-color:#2c2c2c;
								                        border-radius:16px;
								                        padding:32px;
								                        ">
								        <tr>
								            <td style="text-align:center; padding-bottom:24px;">
								            <div style="
								                                font-size:22px;
								                                font-weight:700;
								                                color:#fd5c63;
								                                letter-spacing:0.5px;
								                            ">
								                Cursed Auth
								            </div>
								            </td>
								        </tr>

								        <tr>
								            <td style="
								                            font-size:16px;
								                            line-height:1.6;
								                            color:#ededed;
								                            padding-bottom:16px;
								                            ">
								            Hi <strong>%s</strong>,
								            </td>
								        </tr>

								        <tr>
								            <td style="
								                            font-size:15px;
								                            line-height:1.6;
								                            color:#ededed;
								                            padding-bottom:24px;
								                            ">
								            Use the OTP below to verify your email address. This code is valid for a short time and should not be
								            shared with anyone.
								            </td>
								        </tr>

								        <tr>
								            <td align="center" style="padding-bottom:24px;">
								            <div style="
								                                display:inline-block;
								                                padding:16px 32px;
								                                font-size:28px;
								                                font-weight:700;
								                                letter-spacing:6px;
								                                color:#0a0a0a;
								                                background-color:#fd5c63;
								                                border-radius:12px;
								                            ">
								                %s
								            </div>
								            </td>
								        </tr>

								        <tr>
								            <td align="center" style="padding-bottom:24px;">
								            <a href="https://sso.cursedshrine.co.in/verify?otp=%s" style="
								                                display:inline-block;
								                                background-color:#fd5c63;
								                                color:#0a0a0a;
								                                text-decoration:none;
								                                font-size:15px;
								                                font-weight:700;
								                                padding:14px 28px;
								                                border-radius:10px;
								                                ">
								                Verify Email
								            </a>
								            </td>
								        </tr>

								        <tr>
								            <td style="
								                            font-size:13px;
								                            line-height:1.6;
								                            color:#9a9a9a;
								                            text-align:center;
								                            padding-bottom:24px;
								                            ">
								            Or visit:
								            <br>
								            <a href="https://sso.cursedshrine.co.in/verify?otp=%s" style="color:#fd5c63; text-decoration:none;">
								                https://sso.cursedshrine.co.in/verify?otp=%s
								            </a>
								            </td>
								        </tr>

								        <tr>
								            <td style="
								                            font-size:14px;
								                            line-height:1.6;
								                            color:#bdbdbd;
								                            padding-bottom:24px;
								                            ">
								            If you didn’t request this verification, you can safely ignore this email.
								            </td>
								        </tr>

								        <tr>
								            <td style="
								                            border-top:1px solid #4b4b4b;
								                            padding-top:16px;
								                            font-size:12px;
								                            color:#9a9a9a;
								                            text-align:center;
								                            ">
								            © %d Cursed Auth · Built with clean code & cursed vibes
								            </td>
								        </tr>

								        </table>
								    </td>
								    </tr>
								</table>
								</body>

								</html>
								""",
						request.receiverName(), // 1st %s -> Greeting
						request.OTP(), // 2nd %s -> Big OTP Box
						request.OTP(), // 3rd %s -> Button URL
						request.OTP(), // 4th %s -> "Or visit" href URL
						request.OTP(), // 5th %s -> "Or visit" visible link text
						java.time.Year.now().getValue())); // %d -> Copyright Year

		JsonNode response = restClient.post()
				.body(body)
				.retrieve()
				.body(JsonNode.class);

		CursedLogger.info("SendVerificationOTPEmail API result:" + response.toPrettyString());
		return response;
	}
}
