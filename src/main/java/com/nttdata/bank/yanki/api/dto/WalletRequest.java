package com.nttdata.bank.yanki.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param documentType DNI, CEX or PASSPORT
 * @param documentNumber document number
 * @param phone payment key
 * @param imei device identifier
 * @param email email
 */
public record WalletRequest(
        @NotBlank String documentType,
        @NotBlank String documentNumber,
        @NotBlank String phone,
        @NotBlank String imei,
        @NotBlank String email
) {
}