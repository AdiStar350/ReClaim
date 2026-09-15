```java
public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
    this.mailSenderProvider = mailSenderProvider;
}
```

What that function does: I take an optional mail sender so the app can start even if SMTP is not set up.
What the function is responsible for: I am the constructor. I store the provider for later sends.
Where it works: I am built by Spring as a @Service. AuthService then calls sendOtpEmail and sendPasswordResetEmail.
Why I chose to write the code of the function the way its written: I wrote it this way because objectProvider means a missing JavaMailSender bean does not fail startup.
More: I use Spring ObjectProvider and JavaMailSender.

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

What that function does: I email a 6-digit signup code with a short welcome text.
What the function is responsible for: I am the signup-code email.
Where it works: I am called by AuthService.startRegistration and AuthService.resendOtp.
Why I chose to write the code of the function the way its written: I keep subject and body here and share the send path with password reset.
More: I call sendCodeEmail. The code is put in the body so the user can type it.

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

What that function does: I email a 6-digit password-reset code.
What the function is responsible for: I am the reset-code email.
Where it works: I am called by AuthService.startPasswordReset.
Why I chose to write the code of the function the way its written: I tell the user they can ignore the mail if they did not ask, so a random reset request is less scary.
More: I call sendCodeEmail with a different subject and purpose label.

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
        // Log the code if send fails so the user can still retry.
        log.error("Failed to send {} OTP email to {} — code was {}",
                purpose, to, code, e);
    }
}
```

What that function does: I email the code, or log it when mail is not set up or send fails.
What the function is responsible for: I am the shared send path. I never throw to the caller.
Where it works: I am called by sendOtpEmail and sendPasswordResetEmail. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because logging the code locally lets signup still work without SMTP. A failed send also logs the code so a retry is possible.
More: I use getIfAvailable, SimpleMailMessage, fromAddress from spring.mail.username, and slf4j.
