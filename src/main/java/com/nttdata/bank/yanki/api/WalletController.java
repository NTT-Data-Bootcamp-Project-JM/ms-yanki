package com.nttdata.bank.yanki.api;

import com.nttdata.bank.yanki.application.WalletService;
import com.nttdata.bank.yanki.domain.WalletDocument;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Reactive API of the Yanki wallet.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService wallets;

    /**
     * @param wallets wallet use cases
     */
    public WalletController(WalletService wallets) {
        this.wallets = wallets;
    }

    /**
     * @param request wallet data
     * @return created wallet
     */
    @PostMapping
    public Single<ResponseEntity<WalletDocument>> create(@Valid @RequestBody WalletRequest request) {
        return wallets.create(request.documentType(), request.documentNumber(), request.phone(), request.imei(), request.email())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    /**
     * @return all wallets
     */
    @GetMapping
    public Single<ResponseEntity<List<WalletDocument>>> all() {
        return wallets.findAll().map(ResponseEntity::ok);
    }

    /**
     * @param id wallet identifier
     * @return one wallet
     */
    @GetMapping("/{id}")
    public Single<ResponseEntity<WalletDocument>> one(@PathVariable String id) {
        return wallets.find(id).map(ResponseEntity::ok);
    }

    /**
     * @param id wallet identifier
     * @param request email and device
     * @return updated wallet
     */
    @PutMapping("/{id}")
    public Single<ResponseEntity<WalletDocument>> update(@PathVariable String id, @RequestBody UpdateRequest request) {
        return wallets.update(id, request.email(), request.imei()).map(ResponseEntity::ok);
    }

    /**
     * @param id wallet identifier
     * @return empty response
     */
    @DeleteMapping("/{id}")
    public Single<ResponseEntity<Void>> delete(@PathVariable String id) {
        return wallets.delete(id).map(done -> ResponseEntity.noContent().build());
    }

    /**
     * @param request phones and amount
     * @return sender wallet
     */
    @PostMapping("/transfers")
    public Single<ResponseEntity<WalletDocument>> transfer(@Valid @RequestBody TransferRequest request) {
        return wallets.transfer(request.sourcePhone(), request.targetPhone(), request.amount()).map(ResponseEntity::ok);
    }

    /**
     * @param id wallet identifier
     * @param request debit card number
     * @return linked wallet
     */
    @PostMapping("/{id}/debit-card")
    public Single<ResponseEntity<WalletDocument>> link(@PathVariable String id, @Valid @RequestBody LinkRequest request) {
        return wallets.linkCard(id, request.debitCardNumber()).map(ResponseEntity::ok);
    }

    /**
     * @param id wallet identifier
     * @param request amount to load from the linked account
     * @return accepted operation
     */
    @PostMapping("/{id}/top-ups")
    public Single<ResponseEntity<Map<String, String>>> topUp(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return wallets.topUp(id, request.amount())
                .map(operationId -> ResponseEntity.accepted().body(Map.of("operationId", operationId)));
    }

    /**
     * @param id wallet identifier
     * @param request amount to send to the linked account
     * @return accepted operation
     */
    @PostMapping("/{id}/cash-outs")
    public Single<ResponseEntity<Map<String, String>>> cashOut(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return wallets.cashOut(id, request.amount())
                .map(operationId -> ResponseEntity.accepted().body(Map.of("operationId", operationId)));
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
    public record TransferRequest(@NotBlank String sourcePhone, @NotBlank String targetPhone, @NotNull BigDecimal amount) {
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
}
