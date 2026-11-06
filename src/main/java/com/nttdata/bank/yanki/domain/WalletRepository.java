package com.nttdata.bank.yanki.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Yanki wallets. The phone number is the payment key.
 */
public interface WalletRepository extends MongoRepository<WalletDocument, String> {

    /**
     * @param phone mobile number
     * @return wallet that receives or sends with that number
     */
    Optional<WalletDocument> findByPhone(String phone);
}
