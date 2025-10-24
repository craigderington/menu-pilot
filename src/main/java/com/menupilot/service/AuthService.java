package com.menupilot.service;

import com.menupilot.domain.OtpCode;
import com.menupilot.repo.OtpCodeRepo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
public class AuthService {
    private final OtpCodeRepo otpRepo;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();
    private final HexFormat hex = HexFormat.of();

    public AuthService(OtpCodeRepo otpRepo, EmailService emailService) {
        this.otpRepo = otpRepo;
        this.emailService = emailService;
    }

    public void sendLoginCode(String email, String baseUrl) {
        String code = hex.formatHex(random.generateSeed(3)); // 6 hex chars
        otpRepo.deleteByEmail(email);
        OtpCode otp = new OtpCode();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        otpRepo.save(otp);
        String link = baseUrl + "/auth/verify?email=" + email + "&code=" + code;
        emailService.sendMagicLink(email, link, code);
    }

    public boolean verifyCode(String email, String code) {
        return otpRepo.findByEmailAndCode(email, code)
                .filter(o -> o.getExpiresAt().isAfter(OffsetDateTime.now()))
                .isPresent();
    }
}
