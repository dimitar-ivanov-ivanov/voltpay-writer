package com.voltpay.voltpay_writer.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_core", schema = "write")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentCore {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "id")),
            @AttributeOverride(name = "createdAt", column = @Column(name = "created_at"))
    })
    private PrimaryKey id;

    private BigDecimal amount;

    private Integer status;

    private String currency;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "cust_id")
    private Long custId;

    private String type;
}
