package com.nttdata.bank.yanki.api.dto;

/**
 * @param email email
 * @param imei device identifier
 */
public record UpdateRequest(String email, String imei) {
}