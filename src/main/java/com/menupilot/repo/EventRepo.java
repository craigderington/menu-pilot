package com.menupilot.repo;
import com.menupilot.domain.Event;
import com.menupilot.domain.Org;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EventRepo extends JpaRepository<Event, Long> {
    List<Event> findByOrgOrderByStartsAtDesc(Org org);
}
