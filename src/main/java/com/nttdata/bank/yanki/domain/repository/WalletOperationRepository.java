package com.nttdata.bank.yanki.domain.repository;

import com.nttdata.bank.yanki.domain.model.WalletOperationDocument;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Pending bank-account operations of a wallet.
 */
public interface WalletOperationRepository extends MongoRepository<WalletOperationDocument, String> {
}
