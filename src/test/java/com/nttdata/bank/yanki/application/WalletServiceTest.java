package com.nttdata.bank.yanki.application;

import com.nttdata.bank.yanki.domain.WalletDocument;
import com.nttdata.bank.yanki.domain.WalletOperationDocument;
import com.nttdata.bank.yanki.domain.WalletOperationRepository;
import com.nttdata.bank.yanki.domain.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Wallet use cases with mocked persistence and a mocked Kafka producer.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletServiceTest {

    @Mock
    private WalletRepository wallets;

    @Mock
    private WalletOperationRepository operations;

    @Mock
    private KafkaTemplate<String, String> kafka;

    private final Map<String, WalletDocument> byId = new LinkedHashMap<>();
    private final Map<String, WalletDocument> byPhone = new LinkedHashMap<>();
    private final Map<String, WalletOperationDocument> operationsStore = new LinkedHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();
    private WalletService service;

    @BeforeEach
    void setUp() {
        byId.clear();
        byPhone.clear();
        operationsStore.clear();
        service = new WalletService(wallets, operations, kafka);
        when(wallets.save(any())).thenAnswer(invocation -> {
            WalletDocument wallet = invocation.getArgument(0);
            if (wallet.getId() == null) {
                wallet.setId("wal-" + sequence.incrementAndGet());
            }
            byId.put(wallet.getId(), wallet);
            byPhone.put(wallet.getPhone(), wallet);
            return wallet;
        });
        when(wallets.findById(any())).thenAnswer(invocation -> Optional.ofNullable(byId.get(invocation.getArgument(0))));
        when(wallets.findByPhone(any())).thenAnswer(invocation -> Optional.ofNullable(byPhone.get(invocation.getArgument(0))));
        when(wallets.findAll()).thenAnswer(invocation -> new ArrayList<>(byId.values()));
        doAnswer(invocation -> {
            WalletDocument wallet = invocation.getArgument(0);
            byId.remove(wallet.getId());
            byPhone.remove(wallet.getPhone());
            return null;
        }).when(wallets).delete(any());
        when(operations.save(any())).thenAnswer(invocation -> {
            WalletOperationDocument operation = invocation.getArgument(0);
            operationsStore.put(operation.getOperationId(), operation);
            return operation;
        });
        when(operations.findById(any())).thenAnswer(invocation -> Optional.ofNullable(operationsStore.get(invocation.getArgument(0))));
    }

    @Test
    void createsFindsUpdatesAndDeletesAWallet() {
        WalletDocument created = service.create("DNI", "12345678", "999111222", "imei-1", "ana@wallet.test").blockingGet();

        assertEquals(1, service.findAll().blockingGet().size());
        assertEquals(created.getId(), service.find(created.getId()).blockingGet().getId());
        WalletDocument updated = service.update(created.getId(), "ana@new.test", "imei-2").blockingGet();
        assertEquals("imei-2", updated.getImei());
        assertTrue(service.delete(created.getId()).blockingGet());
    }

    @Test
    void rejectsARepeatedPhoneAndABankDocument() {
        service.create("DNI", "12345678", "999111222", "imei-1", "ana@wallet.test").blockingGet();

        assertThrows(IllegalArgumentException.class,
                () -> service.create("CEX", "AB12", "999111222", "imei-2", "other@wallet.test").blockingGet());
        assertThrows(IllegalArgumentException.class,
                () -> service.create("RUC", "20111", "999000111", "imei-3", "co@wallet.test").blockingGet());
    }

    @Test
    void transfersByPhone() {
        WalletDocument source = service.create("DNI", "1", "111", "imei-1", "a@wallet.test").blockingGet();
        WalletDocument target = service.create("PASSPORT", "2", "222", "imei-2", "b@wallet.test").blockingGet();
        source.setBalance(new BigDecimal("30"));

        WalletDocument after = service.transfer("111", "222", new BigDecimal("10")).blockingGet();

        assertEquals(0, new BigDecimal("20.00").compareTo(after.getBalance()));
        assertEquals(0, new BigDecimal("10.00").compareTo(service.find(target.getId()).blockingGet().getBalance()));
    }

    @Test
    void topUpCreditsTheWalletOnlyWhenTheDebitIsApplied() {
        WalletDocument wallet = service.create("DNI", "1", "111", "imei-1", "a@wallet.test").blockingGet();
        service.linkCard(wallet.getId(), "4111111111111111").blockingGet();

        String operationId = service.topUp(wallet.getId(), new BigDecimal("15")).blockingGet();

        assertEquals(0, BigDecimal.ZERO.compareTo(service.find(wallet.getId()).blockingGet().getBalance()));
        verify(kafka).send(eq("yanki.wallet.operation"), eq(operationId), org.mockito.ArgumentMatchers.contains("DEBIT"));
        service.complete(operationId, "APPLIED");
        assertEquals(0, new BigDecimal("15.00").compareTo(service.find(wallet.getId()).blockingGet().getBalance()));
        service.complete(operationId, "APPLIED");
        assertEquals(0, new BigDecimal("15.00").compareTo(service.find(wallet.getId()).blockingGet().getBalance()));
    }

    @Test
    void cashOutDebitsImmediatelyAndRefundsARejectedCredit() {
        WalletDocument wallet = service.create("DNI", "1", "111", "imei-1", "a@wallet.test").blockingGet();
        wallet.setBalance(new BigDecimal("40"));
        service.linkCard(wallet.getId(), "4111111111111111").blockingGet();

        String operationId = service.cashOut(wallet.getId(), new BigDecimal("12")).blockingGet();

        assertEquals(0, new BigDecimal("28.00").compareTo(service.find(wallet.getId()).blockingGet().getBalance()));
        verify(kafka, times(1)).send(eq("yanki.wallet.operation"), eq(operationId), org.mockito.ArgumentMatchers.contains("CREDIT"));
        service.complete(operationId, "REJECTED");
        assertEquals(0, new BigDecimal("40.00").compareTo(service.find(wallet.getId()).blockingGet().getBalance()));
    }

    @Test
    void topUpRequiresALinkedCardAndAPositiveAmount() {
        WalletDocument wallet = service.create("DNI", "1", "111", "imei-1", "a@wallet.test").blockingGet();

        assertThrows(IllegalArgumentException.class, () -> service.topUp(wallet.getId(), new BigDecimal("5")).blockingGet());
        service.linkCard(wallet.getId(), "4111111111111111").blockingGet();
        assertThrows(IllegalArgumentException.class, () -> service.topUp(wallet.getId(), BigDecimal.ZERO).blockingGet());
    }

    @Test
    void aWalletWithBalanceCannotBeDeleted() {
        WalletDocument wallet = service.create("DNI", "1", "111", "imei-1", "a@wallet.test").blockingGet();
        wallet.setBalance(BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class, () -> service.delete(wallet.getId()).blockingGet());
    }
}
