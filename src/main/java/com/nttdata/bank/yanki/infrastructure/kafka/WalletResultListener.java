package com.nttdata.bank.yanki.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bank.yanki.service.WalletService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Receives the account service result and updates the wallet balance.
 */
@Component
public class WalletResultListener {

    private final WalletService wallets;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * @param wallets wallet use cases
     */
    public WalletResultListener(WalletService wallets) {
        this.wallets = wallets;
    }

    /**
     * @param payload JSON result
     */
    @KafkaListener(topics = "yanki.wallet.result")
    public void onResult(String payload) throws Exception {
        JsonNode node = json.readTree(payload);
        wallets.complete(node.get("operationId").asText(), node.get("status").asText());
    }
}
