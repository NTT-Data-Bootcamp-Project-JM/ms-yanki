package com.nttdata.bank.yanki.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Pending bank-account operations of a wallet.
 */
public interface WalletOperationRepository extends MongoRepository<WalletOperationDocument, String> {
}
