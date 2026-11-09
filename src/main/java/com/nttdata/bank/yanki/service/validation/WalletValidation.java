package com.nttdata.bank.yanki.service.validation;

import java.math.BigDecimal;

/**
 * Yanki accepts DNI, CEX and passport. The holder does not have to be a bank customer.
 */
public final class WalletValidation {

    private WalletValidation() {
    }

    /**
     * @param documentType DNI, CEX or PASSPORT
     */
    public static void checkDocument(String documentType) {
        if (!"DNI".equals(documentType) && !"CEX".equals(documentType) && !"PASSPORT".equals(documentType)) {
            throw new IllegalArgumentException("Yanki accepts only DNI, CEX or PASSPORT");
        }
    }

    /**
     * @param balance current wallet balance
     * @param amount amount to send
     */
    public static void checkBalance(BigDecimal balance, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be greater than zero");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("The wallet does not have enough balance");
        }
    }
}
