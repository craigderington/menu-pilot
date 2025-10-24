package com.menupilot.service;

import com.menupilot.domain.Event;
import com.menupilot.domain.Org;
import com.menupilot.domain.User;
import com.menupilot.repo.EventRepo;
import com.menupilot.repo.OrgRepo;
import com.menupilot.repo.UserRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReminderService {
    private final EventRepo eventRepo;
    private final UserRepo userRepo;
    private final OrgRepo orgRepo;
    private final EmailService emailService;

    // keep simple "already sent" memory in-process; for prod store in DB
    private final Set<String> sentKeys = new HashSet<>();

    public ReminderService(EventRepo eventRepo, UserRepo userRepo, OrgRepo orgRepo, EmailService emailService) {
        this.eventRepo = eventRepo;
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.emailService = emailService;
    }

    // Run hourly
    @Scheduled(cron = "0 0 * * * *")
    public void hourly() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime in24h = now.plusHours(24);
        List<Event> events = eventRepo.findAll();
        for (Event e : events) {
            if (e.getStartsAt() != null && e.getStartsAt().isAfter(now) && e.getStartsAt().isBefore(in24h)) {
                sendForEvent(e, "start");
            }
            if (e.getCutoffAt() != null && e.getCutoffAt().isAfter(now) && e.getCutoffAt().isBefore(in24h)) {
                sendForEvent(e, "cutoff");
            }
        }
    }

    private void sendForEvent(Event e, String kind) {
        String key = kind + ":" + e.getId() + ":" + e.getStartsAt();
        if (sentKeys.contains(key)) return;
        Org org = e.getOrg();
        String eventUrl = "/events/" + e.getId();
        String cutoff = e.getCutoffAt() == null ? null : e.getCutoffAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        for (User u : userRepo.findAll()) {
            if (!u.isActive()) continue;
            if (u.getOrg().getId().equals(org.getId())) {
                emailService.sendEventReminder(u.getEmail(), e.getName(), eventUrl, cutoff);
            }
        }
        sentKeys.add(key);
    }
}
