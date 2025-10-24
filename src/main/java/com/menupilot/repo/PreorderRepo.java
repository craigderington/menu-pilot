package com.menupilot.repo;
import com.menupilot.domain.Preorder;
import com.menupilot.domain.Event;
import com.menupilot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PreorderRepo extends JpaRepository<Preorder, Long> {
    Optional<Preorder> findByEventAndUser(Event event, User user);
    List<Preorder> findByEvent(Event event);
}
