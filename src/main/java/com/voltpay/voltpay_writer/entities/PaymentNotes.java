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

@Entity
@Table(name = "payment_notes", schema = "write")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PaymentNotes {

    @EmbeddedId
    @AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id")),
        @AttributeOverride(name = "created_at", column = @Column(name = "created_at"))
    })
    private PrimaryKey id;

    @Column(name = "external_ref_id")
    private String externalRefId;

    private String comment;
}
