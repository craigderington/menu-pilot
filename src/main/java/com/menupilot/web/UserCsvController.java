package com.menupilot.web;

import com.menupilot.domain.Org;
import com.menupilot.domain.User;
import com.menupilot.repo.OrgRepo;
import com.menupilot.repo.UserRepo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequestMapping("/export/users")
public class UserCsvController {
    private final UserRepo userRepo;
    private final OrgRepo orgRepo;

    public UserCsvController(UserRepo userRepo, OrgRepo orgRepo) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
    }

    private Org currentOrg(HttpSession session) {
        Long orgId = (Long) session.getAttribute("orgId");
        return orgRepo.findById(orgId).orElseThrow();
    }

    @GetMapping(".csv")
    public ResponseEntity<byte[]> exportUsers(HttpSession session) throws IOException {
        Org org = currentOrg(session);
        List<User> users = userRepo.findAll(); // MVP: single org
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("email","role","active"));
        for (User u : users) {
            csv.printRecord(u.getEmail(), u.getRole().name(), u.isActive());
        }
        csv.flush();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(baos.toByteArray());
    }

    record PreviewUser(String email, String role, Boolean active, String error) {}

    @PostMapping("/import/preview")
    public String preview(HttpSession session, @RequestParam("file") MultipartFile file, Model model) throws IOException {
        Org org = currentOrg(session);
        Reader in = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
        List<PreviewUser> rows = new ArrayList<>();
        boolean hasErrors = false;
        for (CSVRecord r : records) {
            String email = r.isMapped("email") ? r.get("email") : null;
            String role = r.isMapped("role") ? r.get("role") : "MEMBER";
            String activeStr = r.isMapped("active") ? r.get("active") : "true";
            Boolean active = !"false".equalsIgnoreCase(activeStr);
            String error = null;
            if (email == null || email.isBlank()) error = "email is required";
            try { User.Role.valueOf(role.toUpperCase()); } catch(Exception ex) { error = (error==null?"":"; ") + "role invalid"; }
            rows.add(new PreviewUser(email, role.toUpperCase(), active, error));
            if (error != null) hasErrors = true;
        }
        model.addAttribute("rows", rows);
        model.addAttribute("hasErrors", hasErrors);
        model.addAttribute("payload", new com.google.gson.Gson().toJson(rows));
        return "admin/users-import-preview";
    }

    @PostMapping("/import/commit")
    public String commit(HttpSession session, @RequestParam("payload") String payload) {
        Org org = currentOrg(session);
        PreviewUser[] rows = new com.google.gson.Gson().fromJson(payload, PreviewUser[].class);
        for (PreviewUser r : rows) {
            if (r.error() != null) continue;
            User u = userRepo.findByEmail(r.email()).orElse(null);
            if (u == null) {
                u = new User();
                u.setEmail(r.email());
                u.setOrg(org);
            }
            u.setRole(User.Role.valueOf(r.role()));
            u.setActive(Boolean.TRUE.equals(r.active()));
            userRepo.save(u);
        }
        return "redirect:/admin/users";
    }
}
