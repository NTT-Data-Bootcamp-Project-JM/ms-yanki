package com.nttdata.bank.yanki.api.dto;

import java.math.BigDecimal;

/**
 * Wallet returned by the HTTP API.
 */
public record WalletResponse(
        String id,
        String documentType,
        String documentNumber,
        String phone,
        String imei,
        String email,
        BigDecimal balance,
        String debitCardNumber
) {
}