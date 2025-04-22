package com.voltpay.voltpay_writer.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "idempotency", schema = "write")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Idempotency {

    @Id
    private String id;

    private LocalDate date;
}
