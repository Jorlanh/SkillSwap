package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.CreateCheckoutSessionRequest;
import br.com.teamss.skillswap.skill_swap.dto.CreateCheckoutSessionResponse;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinLedgerResponse;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinPackageResponse;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinTransferRequest;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinWalletResponse;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SwapCoinWalletService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinPackage;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class SwapCoinWalletController {

    private final SwapCoinWalletService walletService;
    private final UserServiceDTO userService;

    public SwapCoinWalletController(SwapCoinWalletService walletService, UserServiceDTO userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<SwapCoinWalletResponse> getWallet() {
        return ResponseEntity.ok(walletService.getWallet(authenticatedUser().getUserId()));
    }

    @GetMapping("/packages")
    public ResponseEntity<List<SwapCoinPackageResponse>> getPackages() {
        // No modo gratuito, não oferecemos pacotes nem checkout aos usuários.
        if (!walletService.isCommerceEnabled()) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(Arrays.stream(SwapCoinPackage.values()).map(SwapCoinPackageResponse::from).toList());
    }

    @GetMapping("/ledger")
    public ResponseEntity<Page<SwapCoinLedgerResponse>> getLedger(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(walletService.getLedger(authenticatedUser().getUserId(), PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("createdAt").descending())));
    }

    @PostMapping("/checkout-sessions")
    public ResponseEntity<?> createCheckout(@Valid @RequestBody CreateCheckoutSessionRequest request) {
        if (!walletService.isCommerceEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "A plataforma é gratuita e não oferece compra de SwapCoins nesta versão."));
        }
        return ResponseEntity.ok(walletService.createCheckoutSession(authenticatedUser().getUserId(), request.packageCode()));
    }

    @PostMapping("/transfers")
    public ResponseEntity<SwapCoinWalletResponse> transfer(@Valid @RequestBody SwapCoinTransferRequest request) {
        return ResponseEntity.ok(walletService.transfer(authenticatedUser().getUserId(), request));
    }

    /** P2P tip shortcut; the same auditable transfer rules apply. */
    @PostMapping("/tips")
    public ResponseEntity<SwapCoinWalletResponse> tip(@Valid @RequestBody SwapCoinTransferRequest request) {
        if (request.amount() != 5) {
            throw new IllegalArgumentException("A gorjeta SkillSwap tem valor fixo de 5 SwapCoins.");
        }
        SwapCoinTransferRequest tip = new SwapCoinTransferRequest(request.recipientUserId(), 5, "Gorjeta SkillSwap");
        return ResponseEntity.ok(walletService.transfer(authenticatedUser().getUserId(), tip));
    }

    private UserDTO authenticatedUser() {
        return userService.getAuthenticatedUser();
    }
}
