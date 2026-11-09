package com.nttdata.bank.yanki.domain.repository;

import com.nttdata.bank.yanki.domain.model.WalletDocument;

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
