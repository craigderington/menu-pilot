package com.menupilot.repo;
import com.menupilot.domain.Org;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrgRepo extends JpaRepository<Org, Long> { }
