package com.nttdata.bank.yanki.api.controller;

import com.nttdata.bank.yanki.api.contract.WalletApi;
import com.nttdata.bank.yanki.api.dto.AmountRequest;
import com.nttdata.bank.yanki.api.dto.LinkRequest;
import com.nttdata.bank.yanki.api.dto.OperationAccepted;
import com.nttdata.bank.yanki.api.dto.TransferRequest;
import com.nttdata.bank.yanki.api.dto.UpdateRequest;
import com.nttdata.bank.yanki.api.dto.WalletRequest;
import com.nttdata.bank.yanki.api.dto.WalletResponse;
import com.nttdata.bank.yanki.service.WalletService;
import com.nttdata.bank.yanki.mapper.WalletMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implements the Yanki contract generated from {@code openapi/openapi.yaml}.
 */
@RestController
public class WalletController implements WalletApi {

    private final WalletService wallets;
    private final WalletMapper mapper;

    /**
     * @param wallets wallet use cases
     * @param mapper domain-to-DTO mapper
     */
    public WalletController(WalletService wallets, WalletMapper mapper) {
        this.wallets = wallets;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletResponse> create(WalletRequest request) {
        return wallets.create(request.documentType(), request.documentNumber(), request.phone(), request.imei(),
                        request.email())
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body))
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<List<WalletResponse>> all() {
        return wallets.findAll().map(mapper::toResponseList).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletResponse> one(String id) {
        return wallets.find(id).map(mapper::toResponse).map(ResponseEntity::ok).blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletResponse> update(String id, UpdateRequest request) {
        return wallets.update(id, request.email(), request.imei())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .blockingGet();
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
    public ResponseEntity<WalletResponse> transfer(TransferRequest request) {
        return wallets.transfer(request.sourcePhone(), request.targetPhone(), request.amount())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .blockingGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<WalletResponse> link(String id, LinkRequest request) {
        return wallets.linkCard(id, request.debitCardNumber())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .blockingGet();
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
}
