package com.menupilot.web;

import com.menupilot.domain.Org;
import com.menupilot.domain.User;
import com.menupilot.repo.OrgRepo;
import com.menupilot.repo.UserRepo;
import com.menupilot.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepo userRepo;
    private final OrgRepo orgRepo;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    public AuthController(AuthService authService, UserRepo userRepo, OrgRepo orgRepo) {
        this.authService = authService;
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
    }

    @GetMapping("/login")
    public String loginForm() { return "login"; }

    @PostMapping("/send-code")
    public String sendCode(@RequestParam String email, Model model) {
        authService.sendLoginCode(email, baseUrl);
        model.addAttribute("email", email);
        return "check-email";
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String email, @RequestParam String code, HttpSession session, Model model) {
        if (authService.verifyCode(email, code)) {
            Org org = orgRepo.findAll().stream().findFirst().orElseGet(() -> {
                Org o = new Org();
                o.setName("Demo Club");
                o.setPlan("BASIC");
                o.setSubscriptionStatus("inactive");
                return orgRepo.save(o);
            });
            User user = userRepo.findByEmail(email).orElseGet(() -> {
                User u = new User();
                u.setEmail(email);
                u.setOrg(org);
                if (userRepo.count() == 0) { u.setRole(User.Role.ADMIN); }
                return userRepo.save(u);
            });
            session.setAttribute("userEmail", email);
            session.setAttribute("orgId", org.getId());
            session.setAttribute("role", user.getRole().name());
            return "redirect:/";
        }
        model.addAttribute("error", "Invalid or expired code");
        return "login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
