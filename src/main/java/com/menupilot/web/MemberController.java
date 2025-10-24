package com.menupilot.web;

import com.menupilot.domain.*;
import com.menupilot.repo.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MemberController {
    private final OrgRepo orgRepo;
    private final EventRepo eventRepo;
    private final MenuItemRepo menuItemRepo;
    private final UserRepo userRepo;
    private final PreorderRepo preorderRepo;
    private final PreorderItemRepo preorderItemRepo;

    public MemberController(OrgRepo orgRepo, EventRepo eventRepo, MenuItemRepo menuItemRepo, UserRepo userRepo, PreorderRepo preorderRepo, PreorderItemRepo preorderItemRepo) {
        this.orgRepo = orgRepo;
        this.eventRepo = eventRepo;
        this.menuItemRepo = menuItemRepo;
        this.userRepo = userRepo;
        this.preorderRepo = preorderRepo;
        this.preorderItemRepo = preorderItemRepo;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        model.addAttribute("email", email);
        Org org = orgRepo.findAll().stream().findFirst().orElse(null);
        if (org != null) {
            model.addAttribute("events", eventRepo.findByOrgOrderByStartsAtDesc(org));
        }
        return "index";
    }

    @GetMapping("/events/{id}")
    public String viewEvent(@PathVariable Long id, HttpSession session, Model model) {
        Event e = eventRepo.findById(id).orElseThrow();
        model.addAttribute("event", e);
        List<MenuItem> items = menuItemRepo.findByEvent(e);
        model.addAttribute("items", items);

        // Remaining per item (cap - current committed)
        Map<Long, Integer> remaining = new HashMap<>();
        Map<Long, Integer> currentTotals = currentTotals(e);
        for (MenuItem i : items) {
            Integer cap = i.getCapQty();
            if (cap == null) {
                remaining.put(i.getId(), null); // unlimited
            } else {
                int used = currentTotals.getOrDefault(i.getId(), 0);
                remaining.put(i.getId(), Math.max(cap - used, 0));
            }
        }
        model.addAttribute("remaining", remaining);

        // Cutoff flag
        boolean cutoffPassed = e.getCutoffAt() != null && OffsetDateTime.now().isAfter(e.getCutoffAt());
        model.addAttribute("cutoffPassed", cutoffPassed);

        return "event";
    }

    @PostMapping("/events/{id}/preorder")
    public String preorder(@PathVariable Long id, HttpSession session,
                           @RequestParam Long[] itemId, @RequestParam Integer[] qty,
                           Model model) {
        String email = (String) session.getAttribute("userEmail");
        if (email == null) return "redirect:/auth/login";
        Event e = eventRepo.findById(id).orElseThrow();

        // Cutoff enforcement
        if (e.getCutoffAt() != null && java.time.OffsetDateTime.now().isAfter(e.getCutoffAt())) {
            return "redirect:/events/" + id + "?error=cutoff";
        }

        User u = userRepo.findByEmail(email).orElseThrow();
        Preorder existing = preorderRepo.findByEventAndUser(e, u).orElse(null);

        // Build current totals excluding this user's existing preorder (so they can edit)
        Map<Long, Integer> totalsExclUser = currentTotals(e);
        if (existing != null) {
            for (PreorderItem it : preorderItemRepo.findByPreorder(existing)) {
                totalsExclUser.merge(it.getMenuItem().getId(), -it.getQty(), Integer::sum);
            }
        }

        // Validate caps
        for (int i = 0; i < itemId.length; i++) {
            Long mid = itemId[i];
            int q = qty[i] == null ? 0 : qty[i];
            if (q <= 0) continue;
            MenuItem mi = menuItemRepo.findById(mid).orElseThrow();
            if (mi.getCapQty() != null) {
                int used = Math.max(0, totalsExclUser.getOrDefault(mid, 0));
                int remaining = mi.getCapQty() - used;
                if (q > remaining) {
                    return "redirect:/events/" + id + "?error=cap&item=" + mid + "&rem=" + remaining;
                }
            }
        }

        // Upsert preorder items
        Preorder po = existing;
        if (po == null) {
            po = new Preorder();
            po.setEvent(e);
            po.setUser(u);
            po = preorderRepo.save(po);
        }
        preorderItemRepo.findByPreorder(po).forEach(preorderItemRepo::delete);
        for (int i=0; i<itemId.length; i++) {
            int q = qty[i] == null ? 0 : qty[i];
            if (q > 0) {
                PreorderItem it = new PreorderItem();
                it.setPreorder(po);
                it.setMenuItem(menuItemRepo.findById(itemId[i]).orElseThrow());
                it.setQty(q);
                preorderItemRepo.save(it);
            }
        }
        return "redirect:/events/" + id + "?saved=1";
    }

    private Map<Long, Integer> currentTotals(Event e) {
        Map<Long, Integer> totals = new HashMap<>();
        List<Preorder> pos = preorderRepo.findByEvent(e);
        for (Preorder p : pos) {
            for (PreorderItem it : preorderItemRepo.findByPreorder(p)) {
                Long mid = it.getMenuItem().getId();
                totals.merge(mid, it.getQty(), Integer::sum);
            }
        }
        return totals;
    }
}
