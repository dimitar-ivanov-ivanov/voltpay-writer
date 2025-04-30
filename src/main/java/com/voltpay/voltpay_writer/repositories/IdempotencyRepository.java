package com.voltpay.voltpay_writer.repositories;

import com.voltpay.voltpay_writer.entities.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<Idempotency, String> {
}
