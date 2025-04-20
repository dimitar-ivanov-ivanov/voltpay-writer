package com.voltpay.voltpay_writer.repositories;

import com.voltpay.voltpay_writer.entities.PaymentNotes;
import com.voltpay.voltpay_writer.entities.PrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentNotesRepository extends JpaRepository<PaymentNotes, PrimaryKey> {
}
