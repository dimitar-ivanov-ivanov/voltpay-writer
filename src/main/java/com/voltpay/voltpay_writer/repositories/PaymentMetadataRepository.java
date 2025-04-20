package com.voltpay.voltpay_writer.repositories;

import com.voltpay.voltpay_writer.entities.PaymentMetadata;
import com.voltpay.voltpay_writer.entities.PrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMetadataRepository extends JpaRepository<PaymentMetadata, PrimaryKey> {
}
