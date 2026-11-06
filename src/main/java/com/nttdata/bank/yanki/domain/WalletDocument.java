package com.nttdata.bank.yanki.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * Mobile wallet. A bank customer record is not required.
 */
@Getter
@Setter
@Document("wallets")
public class WalletDocument {

    @Id
    private String id;
    private String documentType;
    private String documentNumber;

    @Indexed(unique = true)
    private String phone;
    private String imei;
    private String email;
    private BigDecimal balance = BigDecimal.ZERO;
    private String debitCardNumber;
}
