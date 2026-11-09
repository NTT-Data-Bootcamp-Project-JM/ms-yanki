package com.nttdata.bank.yanki.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param sourcePhone sender phone
 * @param targetPhone receiver phone
 * @param amount amount
 */
public record TransferRequest(
        @NotBlank String sourcePhone,
        @NotBlank String targetPhone,
        @NotNull BigDecimal amount
) {
}