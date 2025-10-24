package com.menupilot.billing;

import com.menupilot.domain.Org;
import com.menupilot.repo.OrgRepo;
import com.stripe.model.CheckoutSession;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stripe/webhook")
public class StripeWebhookController {

    @Value("${stripe.webhookSecret:}")
    private String endpointSecret;

    private final OrgRepo orgRepo;

    public StripeWebhookController(OrgRepo orgRepo) {
        this.orgRepo = orgRepo;
    }

    @PostMapping
    public ResponseEntity<String> handle(@RequestHeader(value="Stripe-Signature", required=false) String sig,
                                         @RequestBody String payload) {
        Event event;
        if (endpointSecret == null || endpointSecret.isBlank()) {
            event = Event.GSON.fromJson(payload, Event.class);
        } else {
            try {
                event = Webhook.constructEvent(payload, sig, endpointSecret);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid signature");
            }
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> onCheckoutCompleted(event);
                case "customer.subscription.updated" -> onSubscriptionUpdated(event);
                case "customer.subscription.deleted" -> onSubscriptionDeleted(event);
                default -> {}
            }
        } catch (Exception e) {
            return ResponseEntity.ok("ok (error handled)");
        }
        return ResponseEntity.ok("ok");
    }

    private void onCheckoutCompleted(Event event) throws Exception {
        CheckoutSession session = (CheckoutSession) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return;
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();
        Org org = orgRepo.findAll().stream().findFirst().orElse(null);
        if (org == null) return;
        org.setStripeCustomerId(customerId);
        org.setStripeSubscriptionId(subscriptionId);
        org.setSubscriptionStatus("active");
        if (org.getPlan() == null) org.setPlan("BASIC");
        orgRepo.save(org);
    }

    private void onSubscriptionUpdated(Event event) throws Exception {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (sub == null) return;
        String status = sub.getStatus();
        String subId = sub.getId();
        orgRepo.findAll().stream()
            .filter(o -> subId.equals(o.getStripeSubscriptionId()))
            .findFirst()
            .ifPresent(o -> { o.setSubscriptionStatus(status); orgRepo.save(o); });
    }

    private void onSubscriptionDeleted(Event event) throws Exception {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (sub == null) return;
        String subId = sub.getId();
        orgRepo.findAll().stream()
            .filter(o -> subId.equals(o.getStripeSubscriptionId()))
            .findFirst()
            .ifPresent(o -> { o.setSubscriptionStatus("canceled"); orgRepo.save(o); });
    }
}
