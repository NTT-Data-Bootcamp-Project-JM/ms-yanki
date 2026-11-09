package com.nttdata.bank.yanki.service.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Document and balance rules of the mobile wallet.
 */
class WalletValidationTest {

    @Test
    void onlyIdentityDocumentsAreAccepted() {
        WalletValidation.checkDocument("DNI");
        WalletValidation.checkDocument("CEX");
        WalletValidation.checkDocument("PASSPORT");
        assertThrows(IllegalArgumentException.class, () -> WalletValidation.checkDocument("RUC"));
    }

    @Test
    void transferCannotExceedBalance() {
        assertThrows(IllegalArgumentException.class,
                () -> WalletValidation.checkBalance(new BigDecimal("10.00"), new BigDecimal("10.01")));
    }
}
