package com.menupilot.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
    private final TemplateEngine templateEngine;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    // DEV STUB: replace with JavaMailSender in prod
    public void sendRaw(String to, String subject, String html) {
        System.out.println("=== DEV EMAIL (HTML) ===");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println(html);
        System.out.println("========================");
    }

    public void sendMagicLink(String to, String link, String code) {
        Context ctx = new Context();
        ctx.setVariable("link", link);
        ctx.setVariable("code", code);
        String html = templateEngine.process("email/magic-link.html", ctx);
        sendRaw(to, "Your MenuPilot sign-in link", html);
    }

    public void sendEventReminder(String to, String eventName, String eventUrl, String cutoffAt) {
        Context ctx = new Context();
        ctx.setVariable("eventName", eventName);
        ctx.setVariable("eventUrl", eventUrl);
        ctx.setVariable("cutoffAt", cutoffAt);
        String html = templateEngine.process("email/event-reminder.html", ctx);
        sendRaw(to, "Reminder: " + eventName, html);
    }

    // Back-compat for earlier calls
    public void send(String to, String subject, String text) {
        sendRaw(to, subject, "<pre>" + text + "</pre>");
    }
}
