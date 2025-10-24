package com.menupilot.repo;
import com.menupilot.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface OtpCodeRepo extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByEmailAndCode(String email, String code);
    void deleteByEmail(String email);
}
