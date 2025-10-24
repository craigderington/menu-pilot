package com.menupilot.repo;
import com.menupilot.domain.MenuItem;
import com.menupilot.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MenuItemRepo extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByEvent(Event event);
}
