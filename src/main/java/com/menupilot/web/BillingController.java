package com.menupilot.web;

import com.menupilot.domain.Org;
import com.menupilot.repo.OrgRepo;
import com.menupilot.service.SubscriptionService;
import com.stripe.exception.StripeException;
import com.stripe.model.BillingPortal;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final SubscriptionService subscriptionService;
    private final OrgRepo orgRepo;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    public BillingController(SubscriptionService subscriptionService, OrgRepo orgRepo) {
        this.subscriptionService = subscriptionService;
        this.orgRepo = orgRepo;
    }

    private Org currentOrg(HttpSession session) {
        Long orgId = (Long) session.getAttribute("orgId");
        return orgRepo.findById(orgId).orElseThrow();
    }

    @GetMapping
    public String billingHome(HttpSession session, Model model) {
        Org org = currentOrg(session);
        model.addAttribute("org", org);
        model.addAttribute("configured", subscriptionService.isConfigured());
        return "billing";
    }

    @GetMapping("/subscribe")
    public String subscribe(HttpSession session) throws StripeException {
        Org org = currentOrg(session);
        Session s = subscriptionService.createCheckoutSession(org.getId(), baseUrl + "/billing/success", baseUrl + "/billing");
        return "redirect:" + s.getUrl();
    }

    @GetMapping("/portal")
    public String portal(HttpSession session) throws StripeException {
        Org org = currentOrg(session);
        BillingPortal.Session p = subscriptionService.createBillingPortalSession(org.getId(), baseUrl + "/billing");
        return "redirect:" + p.getUrl();
    }

    @GetMapping("/success")
    public String success(Model model) {
        model.addAttribute("message", "Subscription updated. It may take a moment to reflect.");
        return "billing";
    }
}
