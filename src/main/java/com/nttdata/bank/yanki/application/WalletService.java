package com.nttdata.bank.yanki.application;

import com.nttdata.bank.yanki.domain.WalletDocument;
import com.nttdata.bank.yanki.domain.WalletOperationDocument;
import com.nttdata.bank.yanki.domain.WalletOperationRepository;
import com.nttdata.bank.yanki.domain.WalletRepository;
import com.nttdata.bank.yanki.domain.WalletRules;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Yanki payments by phone. Bank-account movements are requested through Kafka, never through REST.
 */
@Service
public class WalletService {

    private final WalletRepository wallets;
    private final WalletOperationRepository operations;
    private final KafkaTemplate<String, String> kafka;

    /**
     * @param wallets wallet collection
     * @param operations pending bank operations
     * @param kafka Kafka producer
     */
    public WalletService(WalletRepository wallets, WalletOperationRepository operations, KafkaTemplate<String, String> kafka) {
        this.wallets = wallets;
        this.operations = operations;
        this.kafka = kafka;
    }

    /**
     * @param documentType DNI, CEX or PASSPORT
     * @param documentNumber document number
     * @param phone payment key
     * @param imei device identifier
     * @param email email
     * @return created wallet
     */
    public Single<WalletDocument> create(String documentType, String documentNumber, String phone, String imei, String email) {
        return Single.fromCallable(() -> {
            WalletRules.checkDocument(documentType);
            if (wallets.findByPhone(phone).isPresent()) {
                throw new IllegalArgumentException("That phone already has a wallet");
            }
            WalletDocument wallet = new WalletDocument();
            wallet.setDocumentType(documentType);
            wallet.setDocumentNumber(documentNumber);
            wallet.setPhone(phone);
            wallet.setImei(imei);
            wallet.setEmail(email);
            wallet.setBalance(BigDecimal.ZERO);
            return wallets.save(wallet);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * @return all wallets
     */
    public Single<List<WalletDocument>> findAll() {
        return Single.fromCallable(wallets::findAll).subscribeOn(Schedulers.io());
    }

    /**
     * @param id wallet identifier
     * @return one wallet
     */
    public Single<WalletDocument> find(String id) {
        return Single.fromCallable(() -> load(id)).subscribeOn(Schedulers.io());
    }

    /**
     * @param id wallet identifier
     * @param email new email
     * @param imei new device
     * @return updated wallet
     */
    public Single<WalletDocument> update(String id, String email, String imei) {
        return Single.fromCallable(() -> {
            WalletDocument wallet = load(id);
            wallet.setEmail(email);
            wallet.setImei(imei);
            return wallets.save(wallet);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Deletes a wallet with a zero balance.
     *
     * @param id wallet identifier
     * @return completion
     */
    public Single<Boolean> delete(String id) {
        return Single.fromCallable(() -> {
            WalletDocument wallet = load(id);
            if (wallet.getBalance().signum() != 0) {
                throw new IllegalArgumentException("A wallet with balance cannot be deleted");
            }
            wallets.delete(wallet);
            return true;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Sends money to another wallet using only the phone number.
     *
     * @param sourcePhone sender
     * @param targetPhone receiver
     * @param amount amount
     * @return sender wallet
     */
    public Single<WalletDocument> transfer(String sourcePhone, String targetPhone, BigDecimal amount) {
        return Single.fromCallable(() -> {
            WalletDocument source = wallets.findByPhone(sourcePhone)
                    .orElseThrow(() -> new IllegalArgumentException("Source wallet was not found"));
            WalletDocument target = wallets.findByPhone(targetPhone)
                    .orElseThrow(() -> new IllegalArgumentException("Target wallet was not found"));
            WalletRules.checkBalance(source.getBalance(), amount);
            source.setBalance(source.getBalance().subtract(amount));
            target.setBalance(target.getBalance().add(amount));
            wallets.save(target);
            return wallets.save(source);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Associates the wallet with a debit card. Later loads and unloads use that card's main account.
     *
     * @param id wallet identifier
     * @param debitCardNumber card number
     * @return updated wallet
     */
    public Single<WalletDocument> linkCard(String id, String debitCardNumber) {
        return Single.fromCallable(() -> {
            WalletDocument wallet = load(id);
            wallet.setDebitCardNumber(debitCardNumber);
            return wallets.save(wallet);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Asks the account service to debit the card's main account. The wallet balance grows when Kafka confirms it.
     *
     * @param id wallet identifier
     * @param amount amount to load
     * @return operation identifier
     */
    public Single<String> topUp(String id, BigDecimal amount) {
        return Single.fromCallable(() -> publish(load(id), amount, "DEBIT")).subscribeOn(Schedulers.io());
    }

    /**
     * Takes money from the wallet and asks the account service to credit the card's main account.
     *
     * @param id wallet identifier
     * @param amount amount
     * @return operation identifier
     */
    public Single<String> cashOut(String id, BigDecimal amount) {
        return Single.fromCallable(() -> {
            WalletDocument wallet = load(id);
            WalletRules.checkBalance(wallet.getBalance(), amount);
            wallet.setBalance(wallet.getBalance().subtract(amount));
            wallets.save(wallet);
            return publish(wallet, amount, "CREDIT");
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Completes or rolls back a bank operation.
     *
     * @param operationId operation identifier
     * @param status APPLIED or REJECTED
     */
    public void complete(String operationId, String status) {
        WalletOperationDocument operation = operations.findById(operationId).orElse(null);
        if (operation == null || !"PENDING".equals(operation.getStatus())) {
            return;
        }
        WalletDocument wallet = load(operation.getWalletId());
        if ("APPLIED".equals(status) && "DEBIT".equals(operation.getDirection())) {
            wallet.setBalance(wallet.getBalance().add(operation.getAmount()));
            wallets.save(wallet);
        }
        if ("REJECTED".equals(status) && "CREDIT".equals(operation.getDirection())) {
            wallet.setBalance(wallet.getBalance().add(operation.getAmount()));
            wallets.save(wallet);
        }
        operation.setStatus(status);
        operations.save(operation);
    }

    private String publish(WalletDocument wallet, BigDecimal amount, String direction) {
        if (wallet.getDebitCardNumber() == null || wallet.getDebitCardNumber().isBlank()) {
            throw new IllegalArgumentException("The wallet needs a linked debit card");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("The amount must be greater than zero");
        }
        String operationId = UUID.randomUUID().toString();
        WalletOperationDocument operation = new WalletOperationDocument();
        operation.setOperationId(operationId);
        operation.setWalletId(wallet.getId());
        operation.setAmount(amount);
        operation.setDirection(direction);
        operation.setStatus("PENDING");
        operations.save(operation);
        String payload = "{\"operationId\":\"" + operationId + "\",\"debitCardNumber\":\"" + wallet.getDebitCardNumber()
                + "\",\"amount\":\"" + amount.toPlainString() + "\",\"direction\":\"" + direction + "\"}";
        kafka.send("yanki.wallet.operation", operationId, payload);
        return operationId;
    }

    private WalletDocument load(String id) {
        return wallets.findById(id).orElseThrow(() -> new IllegalArgumentException("Wallet was not found"));
    }
}
