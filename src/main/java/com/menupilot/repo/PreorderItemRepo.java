package com.menupilot.repo;
import com.menupilot.domain.PreorderItem;
import com.menupilot.domain.Preorder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PreorderItemRepo extends JpaRepository<PreorderItem, Long> {
    List<PreorderItem> findByPreorder(Preorder preorder);
}
