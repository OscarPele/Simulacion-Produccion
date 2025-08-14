package com.oscar.proyecto.ms_auth.mail;

public interface MailSenderPort {
    void send(String to, String subject, String htmlBody);
}
