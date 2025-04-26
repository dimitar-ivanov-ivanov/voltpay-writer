package com.voltpay.voltpay_writer.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_core", schema = "write")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
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

    @Column(name = "cust_id")
    private Long custId;

    private String type;
}
