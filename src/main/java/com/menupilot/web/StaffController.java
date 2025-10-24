package com.menupilot.web;

import com.menupilot.domain.Event;
import com.menupilot.repo.EventRepo;
import com.menupilot.service.PrepListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/staff")
public class StaffController {
    private final EventRepo eventRepo;
    private final PrepListService prepListService;

    public StaffController(EventRepo eventRepo, PrepListService prepListService) {
        this.eventRepo = eventRepo;
        this.prepListService = prepListService;
    }

    @GetMapping("/events/{id}/prep-list")
    public String prepList(@PathVariable Long id, Model model) {
        Event e = eventRepo.findById(id).orElseThrow();
        model.addAttribute("event", e);
        model.addAttribute("rows", prepListService.aggregateByStation(e));
        return "prep-list";
    }
}
