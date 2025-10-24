package com.menupilot.web;

import com.menupilot.domain.Org;
import com.menupilot.repo.OrgRepo;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/org")
public class OrgController {
    private final OrgRepo orgRepo;

    @Value("${upload.dir:/tmp/menupilot-uploads}")
    private String uploadDir;

    public OrgController(OrgRepo orgRepo) {
        this.orgRepo = orgRepo;
    }

    private Org currentOrg(HttpSession session) {
        Long orgId = (Long) session.getAttribute("orgId");
        return orgRepo.findById(orgId).orElseThrow();
    }

    @GetMapping
    public String settings(HttpSession session, Model model) {
        Org org = currentOrg(session);
        model.addAttribute("org", org);
        return "admin/org-settings";
    }

    @PostMapping("/logo")
    public String uploadLogo(HttpSession session, @RequestParam("file") MultipartFile file) throws IOException {
        Org org = currentOrg(session);
        if (file != null && !file.isEmpty()) {
            Files.createDirectories(Paths.get(uploadDir, "logos"));
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            if (ext == null) ext = "png";
            Path dest = Paths.get(uploadDir, "logos", "org-" + org.getId() + "." + ext.toLowerCase());
            Files.write(dest, file.getBytes());
            org.setLogoPath(dest.toAbsolutePath().toString());
            orgRepo.save(org);
        }
        return "redirect:/admin/org";
    }

    @GetMapping(value="/logo", produces=MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getLogo(HttpSession session) throws IOException {
        Org org = currentOrg(session);
        if (org.getLogoPath() == null) return ResponseEntity.notFound().build();
        Path p = Paths.get(org.getLogoPath());
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        byte[] bytes = Files.readAllBytes(p);
        // We always say PNG for simplicity; browsers will still render
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes);
    }
}
