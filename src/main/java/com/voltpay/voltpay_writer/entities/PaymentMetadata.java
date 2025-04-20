package com.voltpay.voltpay_writer.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_metadata", schema = "write")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentMetadata {

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "id")),
            @AttributeOverride(name = "created_at", column = @Column(name = "created_at"))
    })
    private PrimaryKey id;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Integer version;
}
