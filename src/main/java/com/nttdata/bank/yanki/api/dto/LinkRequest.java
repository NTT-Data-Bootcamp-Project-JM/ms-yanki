package com.nttdata.bank.yanki.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param debitCardNumber card whose main account will be used
 */
public record LinkRequest(@NotBlank String debitCardNumber) {
}