package com.nttdata.bank.yanki.api;

import com.nttdata.bank.yanki.api.contract.WalletApi;
import com.nttdata.bank.yanki.application.WalletService;
import com.nttdata.bank.yanki.domain.WalletDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implements the Yanki contract generated from {@code openapi/openapi.yaml}.
 */
@RestController
public class WalletController implements WalletApi {

    private final WalletService wallets;

    /**
     * @param wallets wallet use cases
     */
    public WalletController(WalletService wallets) {
        this.wallets = wallets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletDocument> create(WalletRequest request) {
        return wallets.create(request.documentType(), request.documentNumber(), request.phone(), request.imei(),
                        request.email())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<WalletDocument>> all() {
        return wallets.findAll().map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletDocument> one(String id) {
        return wallets.find(id).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletDocument> update(String id, UpdateRequest request) {
        return wallets.update(id, request.email(), request.imei()).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Void> delete(String id) {
        return wallets.delete(id).map(done -> ResponseEntity.noContent().<Void>build()).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletDocument> transfer(TransferRequest request) {
        return wallets.transfer(request.sourcePhone(), request.targetPhone(), request.amount())
                .map(ResponseEntity::ok)
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletDocument> link(String id, LinkRequest request) {
        return wallets.linkCard(id, request.debitCardNumber()).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<OperationAccepted> topUp(String id, AmountRequest request) {
        return wallets.topUp(id, request.amount())
                .map(operationId -> ResponseEntity.accepted().body(new OperationAccepted(operationId)))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<OperationAccepted> cashOut(String id, AmountRequest request) {
        return wallets.cashOut(id, request.amount())
                .map(operationId -> ResponseEntity.accepted().body(new OperationAccepted(operationId)))
                .blockingGet();
    }

    /**
     * @param documentType DNI, CEX or PASSPORT
     * @param documentNumber document number
     * @param phone payment key
     * @param imei device identifier
     * @param email email
     */
    public record WalletRequest(@NotBlank String documentType, @NotBlank String documentNumber, @NotBlank String phone,
                                @NotBlank String imei, @NotBlank String email) {
    }

    /**
     * @param email email
     * @param imei device identifier
     */
    public record UpdateRequest(String email, String imei) {
    }

    /**
     * @param sourcePhone sender phone
     * @param targetPhone receiver phone
     * @param amount amount
     */
    public record TransferRequest(@NotBlank String sourcePhone, @NotBlank String targetPhone,
                                  @NotNull BigDecimal amount) {
    }

    /**
     * @param debitCardNumber card whose main account will be used
     */
    public record LinkRequest(@NotBlank String debitCardNumber) {
    }

    /**
     * @param amount money amount
     */
    public record AmountRequest(@NotNull BigDecimal amount) {
    }

    /**
     * @param operationId identifier of the accepted wallet operation
     */
    public record OperationAccepted(String operationId) {
    }
}
