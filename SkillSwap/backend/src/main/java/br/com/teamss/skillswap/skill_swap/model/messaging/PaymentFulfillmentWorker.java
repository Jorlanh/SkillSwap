package br.com.teamss.skillswap.skill_swap.model.messaging;

import br.com.teamss.skillswap.skill_swap.model.services.SwapCoinWalletService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.rabbitmq.enabled", havingValue = "true")
public class PaymentFulfillmentWorker {
    private final SwapCoinWalletService walletService;

    public PaymentFulfillmentWorker(SwapCoinWalletService walletService) { this.walletService = walletService; }

    @RabbitListener(queues = "${app.messaging.rabbitmq.queue:skillswap.payments.fulfillment}")
    public void process(PaymentFulfillmentMessage message) {
        walletService.fulfillPaidCheckout(message.sessionId(), message.amountTotal(), message.currency(), message.paymentIntent());
    }
}
