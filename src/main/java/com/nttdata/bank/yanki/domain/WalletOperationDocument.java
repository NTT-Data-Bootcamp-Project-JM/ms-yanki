package com.nttdata.bank.yanki.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * Wallet operation waiting for the account service to confirm the debit-card movement.
 */
@Getter
@Setter
@Document("wallet_operations")
public class WalletOperationDocument {

    @Id
    private String operationId;
    private String walletId;
    private BigDecimal amount;
    private String direction;
    private String status;
}
