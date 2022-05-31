package io.lcalmsky.app.infra.mail;

public interface EmailService {
    void sendEmail(EmailMessage emailMessage);
}
