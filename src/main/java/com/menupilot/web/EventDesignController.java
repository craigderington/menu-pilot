package com.menupilot.web;

import com.menupilot.domain.Event;
import com.menupilot.repo.EventRepo;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/events/{id}/design")
public class EventDesignController {

    private final EventRepo eventRepo;

    @Value("${upload.dir:/tmp/menupilot-uploads}")
    private String uploadDir;

    public EventDesignController(EventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    @GetMapping
    public String design(@PathVariable Long id, Model model) {
        Event e = eventRepo.findById(id).orElseThrow();
        model.addAttribute("event", e);
        return "admin/event-design";
    }

    @PostMapping("/cover")
    public String uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Event e = eventRepo.findById(id).orElseThrow();
        if (file != null && !file.isEmpty()) {
            Files.createDirectories(Paths.get(uploadDir, "covers"));
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            if (ext == null) ext = "png";
            Path dest = Paths.get(uploadDir, "covers", "event-" + id + "." + ext.toLowerCase());
            Files.write(dest, file.getBytes());
            e.setCoverImagePath(dest.toAbsolutePath().toString());
            eventRepo.save(e);
        }
        return "redirect:/admin/events/" + id + "/design";
    }

    @PostMapping("/theme")
    public String setTheme(@PathVariable Long id, @RequestParam("accent") String accent) {
        Event e = eventRepo.findById(id).orElseThrow();
        e.setThemeAccentHex(accent);
        eventRepo.save(e);
        return "redirect:/admin/events/" + id + "/design";
    }
}
