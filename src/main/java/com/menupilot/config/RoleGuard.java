package com.menupilot.config;

import com.menupilot.domain.Org;
import com.menupilot.repo.OrgRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class RoleGuard implements HandlerInterceptor {

    private final OrgRepo orgRepo;

    public RoleGuard(OrgRepo orgRepo) {
        this.orgRepo = orgRepo;
    }

    private boolean hasRole(HttpSession session, Set<String> allowed) {
        Object role = session != null ? session.getAttribute("role") : null;
        if (role == null) return false;
        return allowed.contains(role.toString());
    }

    private boolean isActiveOrg(HttpSession session) {
        if (session == null) return false;
        Object id = session.getAttribute("orgId");
        if (!(id instanceof Long)) return false;
        Org o = orgRepo.findById((Long) id).orElse(null);
        if (o == null) return false;
        String status = o.getSubscriptionStatus();
        return status != null && status.equalsIgnoreCase("active");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        HttpSession session = request.getSession(false);

        // Require login for member/billing/admin/staff
        if (path.startsWith("/events/") || path.startsWith("/staff/") || path.startsWith("/admin/") || path.startsWith("/billing")) {
            if (session == null || session.getAttribute("userEmail") == null) {
                response.sendRedirect("/auth/login");
                return false;
            }
        }

        // Allow billing even if inactive (so org can subscribe/manage)
        boolean isBilling = path.equals("/billing") || path.startsWith("/billing/");

        // Enforce active subscription for admin & staff sections
        if (!isBilling && (path.startsWith("/admin/") || path.startsWith("/staff/"))) {
            if (!isActiveOrg(session)) {
                response.sendRedirect("/billing");
                return false;
            }
        }

        // Staff routes
        if (path.startsWith("/staff/")) {
            if (!hasRole(session, Set.of("STAFF","ADMIN"))) {
                response.sendError(403);
                return false;
            }
        }

        // Admin routes
        if (path.startsWith("/admin/")) {
            if (!hasRole(session, Set.of("ADMIN"))) {
                response.sendError(403);
                return false;
            }
        }

        return true;
    }
}
