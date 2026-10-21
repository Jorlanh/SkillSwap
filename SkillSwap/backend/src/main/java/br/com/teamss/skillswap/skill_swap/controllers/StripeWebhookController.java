package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.model.services.SwapCoinWalletService;
import br.com.teamss.skillswap.skill_swap.model.services.InstitutionalBillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripeWebhookController {

    private final SwapCoinWalletService walletService;
    private final InstitutionalBillingService institutionalBillingService;

    public StripeWebhookController(SwapCoinWalletService walletService, InstitutionalBillingService institutionalBillingService) {
        this.walletService = walletService;
        this.institutionalBillingService = institutionalBillingService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(
        @RequestBody String rawPayload,
        @RequestHeader("Stripe-Signature") String stripeSignature
    ) {
        walletService.handleStripeWebhook(rawPayload, stripeSignature);
        institutionalBillingService.handleWebhook(rawPayload, stripeSignature);
        return ResponseEntity.ok().build();
    }
}
