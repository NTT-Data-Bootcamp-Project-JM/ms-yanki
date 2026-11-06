package com.nttdata.bank.yanki.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Document and balance rules of the mobile wallet.
 */
class WalletRulesTest {

    @Test
    void onlyIdentityDocumentsAreAccepted() {
        WalletRules.checkDocument("DNI");
        WalletRules.checkDocument("CEX");
        WalletRules.checkDocument("PASSPORT");
        assertThrows(IllegalArgumentException.class, () -> WalletRules.checkDocument("RUC"));
    }

    @Test
    void transferCannotExceedBalance() {
        assertThrows(IllegalArgumentException.class,
                () -> WalletRules.checkBalance(new BigDecimal("10.00"), new BigDecimal("10.01")));
    }
}
