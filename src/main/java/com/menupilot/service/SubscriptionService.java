package com.menupilot.service;

import com.menupilot.domain.Org;
import com.menupilot.repo.OrgRepo;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.BillingPortal;
import com.stripe.model.checkout.Session;
import com.stripe.param.BillingPortal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class SubscriptionService {
    @Value("${stripe.secret:}")
    private String stripeSecret;

    @Value("${stripe.priceId:}")
    private String priceId;

    private final OrgRepo orgRepo;

    public SubscriptionService(OrgRepo orgRepo) {
        this.orgRepo = orgRepo;
    }

    @PostConstruct
    public void init() {
        if (stripeSecret != null && !stripeSecret.isBlank()) {
            Stripe.apiKey = stripeSecret;
        }
    }

    public boolean isConfigured() {
        return stripeSecret != null && !stripeSecret.isBlank() && priceId != null && !priceId.isBlank();
    }

    public Session createCheckoutSession(Long orgId, String successUrl, String cancelUrl) throws StripeException {
        Org org = orgRepo.findById(orgId).orElseThrow();
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(cancelUrl)
            .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPrice(priceId).build());
        if (org.getStripeCustomerId() != null && !org.getStripeCustomerId().isBlank()) {
            builder.setCustomer(org.getStripeCustomerId());
        }
        return Session.create(builder.build());
    }

    public BillingPortal.Session createBillingPortalSession(Long orgId, String returnUrl) throws StripeException {
        Org org = orgRepo.findById(orgId).orElseThrow();
        if (org.getStripeCustomerId() == null || org.getStripeCustomerId().isBlank()) {
            throw new StripeException("No Stripe customer for org", null, null, 0, null, null);
        }
        SessionCreateParams params = SessionCreateParams.builder()
            .setCustomer(org.getStripeCustomerId())
            .setReturnUrl(returnUrl)
            .build();
        return BillingPortal.Session.create(params);
    }
}
