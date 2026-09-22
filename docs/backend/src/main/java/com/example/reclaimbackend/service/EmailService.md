# EmailService.java

## Overview
This is a **service** that sends **OTP** emails for signup and password reset over **SMTP** (see `docs/GLOSSARY.md`). If mail is not configured, it logs the code instead of failing the whole flow, so local development still works. A real send failure (mail configured but broken) becomes an HTTP 500 so the app does not treat it as “server down.”

## Concepts used here
- **Service** — business logic called by other services; see `docs/GLOSSARY.md`.
- **OTP** — the 6-digit code placed in the email body; see `docs/GLOSSARY.md`.
- **SMTP** — how the message is delivered (e.g. Gmail); see `docs/GLOSSARY.md`.
- **Dependency injection** — `JavaMailSender` is optional via `ObjectProvider`; see `docs/GLOSSARY.md`.
- **HTTP status code** — `500` when send fails after SMTP is configured; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Mail** (`JavaMailSender`, `SimpleMailMessage`): builds and sends plain-text emails.
- **Spring** (`@Service`, `@Value`, `ObjectProvider`, `HttpStatus`, `ResponseStatusException`): service bean, optional mail sender, from-address property, and API errors.
- **Lombok** (`@Slf4j`): logger for warnings/errors and the “log the OTP” fallback.

## Fields
- `mailSenderProvider` (`ObjectProvider<JavaMailSender>`) — may or may not contain a mail sender, depending on SMTP config.
- `fromAddress` (`String`) — `spring.mail.username`; used as the From header when sending.

## Functions

### `private void sendCodeEmail(String to, String code, String purpose, String subject, String body)`
```java
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
        log.error("Failed to send {} OTP email to {} — code was {}",
                purpose, to, code, e);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Couldn't send the email. Please try again.");
    }
}
```
- **What it does:** either emails the OTP, logs it when SMTP is missing, or throws `500` if SMTP is present but send fails.
- **Where it's called:** only by `sendOtpEmail` and `sendPasswordResetEmail` in this class.
- **Why it's written this way:** one private path keeps subject/body wording in the public methods while sharing config checks and error handling; logging the code locally avoids blocking signup during student setup.
- **Step by step:**
  1. Try to get a `JavaMailSender`.
  2. If missing or `fromAddress` blank, log the OTP and return.
  3. Build a `SimpleMailMessage` and send it.
  4. On exception, log the error (including the code) and throw `ResponseStatusException` 500.

### `public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider)`
```java
public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
    this.mailSenderProvider = mailSenderProvider;
}
```
- **What it does:** stores the optional mail-sender provider Spring injects.
- **Where it's called:** by Spring when creating this bean.
- **Why it's written this way:** `ObjectProvider` lets the app start even when no `JavaMailSender` bean exists.
- **Step by step:**
  1. Receive the provider from Spring.
  2. Assign it to the field.

### `public void sendOtpEmail(String to, String code)`
```java
public void sendOtpEmail(String to, String code) {
    sendCodeEmail(to, code, "registration",
            "Your ReClaim verification code",
            "Welcome to ReClaim!\n\n"
                    + "Your verification code is: " + code + "\n\n"
                    + "Enter this code in the app to finish creating your account. "
                    + "It will expire shortly. If you didn't request this, you can "
                    + "ignore this email.");
}
```
- **What it does:** sends (or logs) the signup verification email with the given code.
- **Where it's called:** `AuthService.startRegistration` and `AuthService.resendOtp`.
- **Why it's written this way:** keeps registration copy in one place and reuses `sendCodeEmail` for delivery.
- **Step by step:**
  1. Build the registration subject and body including `code`.
  2. Delegate to `sendCodeEmail` with purpose `"registration"`.

### `public void sendPasswordResetEmail(String to, String code)`
```java
public void sendPasswordResetEmail(String to, String code) {
    sendCodeEmail(to, code, "password reset",
            "Your ReClaim password reset code",
            "We received a request to reset your ReClaim password.\n\n"
                    + "Your password reset code is: " + code + "\n\n"
                    + "Enter this code in the app to choose a new password. "
                    + "It will expire shortly. If you didn't request this, you can "
                    + "safely ignore this email — your password won't change.");
}
```
- **What it does:** sends (or logs) the forgot-password email with the given code.
- **Where it's called:** `AuthService.startPasswordReset`.
- **Why it's written this way:** separate wording from signup so users are not confused about which flow the code is for.
- **Step by step:**
  1. Build the password-reset subject and body including `code`.
  2. Delegate to `sendCodeEmail` with purpose `"password reset"`.
