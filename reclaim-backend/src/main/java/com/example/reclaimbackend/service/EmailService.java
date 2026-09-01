package com.example.reclaimbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails (currently registration OTP codes).
 * <p>
 * The {@link JavaMailSender} bean only exists when {@code spring.mail.host}
 * is configured. To keep local development friction-free, this service treats
 * mail as optional: if no sender is available (or a send fails), the OTP is
 * written to the server log instead of being emailed, so the flow can still
 * be exercised end-to-end.
 * </p>
 */
@Service
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    /**
     * Emails a 6-digit verification code to the recipient.
     *
     * @param to   recipient email address
     * @param code the 6-digit one-time code
     */
    public void sendOtpEmail(String to, String code) {
        sendCodeEmail(to, code, "registration",
                "Your ReClaim verification code",
                "Welcome to ReClaim!\n\n"
                        + "Your verification code is: " + code + "\n\n"
                        + "Enter this code in the app to finish creating your account. "
                        + "It will expire shortly. If you didn't request this, you can "
                        + "ignore this email.");
    }

    /**
     * Emails a 6-digit password-reset code to the recipient.
     *
     * @param to   recipient email address
     * @param code the 6-digit one-time code
     */
    public void sendPasswordResetEmail(String to, String code) {
        sendCodeEmail(to, code, "password reset",
                "Your ReClaim password reset code",
                "We received a request to reset your ReClaim password.\n\n"
                        + "Your password reset code is: " + code + "\n\n"
                        + "Enter this code in the app to choose a new password. "
                        + "It will expire shortly. If you didn't request this, you can "
                        + "safely ignore this email — your password won't change.");
    }

    /**
     * Shared sender: emails the code, or logs it when SMTP is not configured.
     */
    private void sendCodeEmail(String to, String code, String purpose,
                               String subject, String body) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress == null || fromAddress.isBlank()) {
            log.warn("SMTP not configured — {} OTP for {} is {} "
                    + "(set MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD to email it)",
                    purpose, to, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("{} OTP email sent to {}", purpose, to);
        } catch (Exception e) {
            // Don't fail the request if email delivery hiccups — log the code so
            // the flow remains testable, and let the user retry/resend.
            log.error("Failed to send {} OTP email to {} — code was {}",
                    purpose, to, code, e);
        }
    }
}
