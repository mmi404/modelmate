package com.modelmate.mail;

import com.modelmate.auth.event.PasswordResetCodeIssued;
import com.modelmate.config.ModelMateProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public MailService(JavaMailSender mailSender, ModelMateProperties props) {
        this.mailSender = mailSender;
        this.enabled = props.mail().enabled();
        this.from = props.mail().from();
    }

    @EventListener
    public void onPasswordResetCode(PasswordResetCodeIssued event) {
        if (!enabled) {
            log.info("[mail disabled] password reset code for {} is {}", event.email(), event.code());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject("Your ModelMate password reset code");
        message.setText("""
                Someone requested a password reset for your ModelMate account.

                Your reset code is: %s

                It expires in 15 minutes. If you didn't request this, you can ignore this email.
                """.formatted(event.code()));
        try {
            mailSender.send(message);
            log.info("Sent password reset code to {}", event.email());
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}: {}", event.email(), ex.getMessage());
        }
    }
}
