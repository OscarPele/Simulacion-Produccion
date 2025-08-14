package com.oscar.proyecto.ms_auth.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SmtpMailSender implements MailSenderPort {

    private final JavaMailSender mailSender;
    private final String from;
    private final String replyTo;

    public SmtpMailSender(JavaMailSender mailSender,
                          @Value("${app.mail.from:no-reply@localhost}") String from,
                          @Value("${app.mail.replyTo:}") @Nullable String replyTo) {
        this.mailSender = mailSender;
        this.from = from;
        this.replyTo = replyTo == null ? "" : replyTo;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            if (!replyTo.isBlank()) helper.setReplyTo(replyTo);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true => HTML
            mailSender.send(msg);
        } catch (Exception ignored) {
        }
    }
}
