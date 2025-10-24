package com.menupilot.web;

import com.menupilot.domain.*;
import com.menupilot.repo.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final OrgRepo orgRepo;
    private final EventRepo eventRepo;
    private final MenuItemRepo menuItemRepo;
    private final UserRepo userRepo;

    public AdminController(OrgRepo orgRepo, EventRepo eventRepo, MenuItemRepo menuItemRepo, UserRepo userRepo) {
        this.orgRepo = orgRepo;
        this.eventRepo = eventRepo;
        this.menuItemRepo = menuItemRepo;
        this.userRepo = userRepo;
    }

    private Org currentOrg(HttpSession session) {
        Long orgId = (Long) session.getAttribute("orgId");
        return orgRepo.findById(orgId).orElseThrow();
    }

    @GetMapping("/events")
    public String listEvents(HttpSession session, Model model) {
        Org org = currentOrg(session);
        model.addAttribute("events", eventRepo.findByOrgOrderByStartsAtDesc(org));
        return "admin/events";
    }

    @PostMapping("/events")
    public String createEvent(HttpSession session, @RequestParam String name,
                              @RequestParam String startsAt, @RequestParam(required=false) String cutoffAt,
                              @RequestParam(required=false) String notes) {
        Org org = currentOrg(session);
        Event e = new Event();
        e.setOrg(org);
        e.setName(name);
        e.setStartsAt(OffsetDateTime.parse(startsAt));
        if (cutoffAt != null && !cutoffAt.isBlank()) e.setCutoffAt(OffsetDateTime.parse(cutoffAt));
        e.setNotes(notes);
        eventRepo.save(e);
        return "redirect:/admin/events";
    }

    @GetMapping("/events/{id}")
    public String eventDetail(@PathVariable Long id, Model model) {
        Event e = eventRepo.findById(id).orElseThrow();
        model.addAttribute("event", e);
        model.addAttribute("items", menuItemRepo.findByEvent(e));
        return "admin/event-detail";
    }

    @PostMapping("/events/{id}/items")
    public String addItem(@PathVariable Long id, @RequestParam String name, @RequestParam(required=false) String description,
                          @RequestParam(required=false) Integer priceCents, @RequestParam(required=false) Integer capQty,
                          @RequestParam(required=false) String station) {
        Event e = eventRepo.findById(id).orElseThrow();
        MenuItem mi = new MenuItem();
        mi.setEvent(e);
        mi.setName(name);
        mi.setDescription(description);
        mi.setPriceCents(priceCents);
        mi.setCapQty(capQty);
        mi.setStation(station);
        menuItemRepo.save(mi);
        return "redirect:/admin/events/" + id;
    }

    @PostMapping("/users/{id}/role")
    public String setRole(@PathVariable Long id, @RequestParam String role) {
        User u = userRepo.findById(id).orElseThrow();
        u.setRole(User.Role.valueOf(role));
        userRepo.save(u);
        return "redirect:/admin/users";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepo.findAll());
        return "admin/users";
    }
}
