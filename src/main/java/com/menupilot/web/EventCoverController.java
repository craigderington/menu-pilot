package com.menupilot.web;

import com.menupilot.domain.Event;
import com.menupilot.repo.EventRepo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/events/{id}/design")
public class EventCoverController {
    private final EventRepo eventRepo;

    public EventCoverController(EventRepo eventRepo) { this.eventRepo = eventRepo; }

    @GetMapping(value="/cover.png", produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> cover(@PathVariable Long id) throws Exception {
        Event e = eventRepo.findById(id).orElseThrow();
        if (e.getCoverImagePath() == null) return ResponseEntity.notFound().build();
        Path p = Paths.get(e.getCoverImagePath());
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        byte[] bytes = Files.readAllBytes(p);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes);
    }
}
