package com.voltpay.voltpay_writer.repositories;

import com.voltpay.voltpay_writer.entities.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface IdempotencyRepository extends JpaRepository<Idempotency, String> {

    @Modifying
    @Query(
        value = "DELETE FROM write.idempotency where date <= :date",
        nativeQuery = true
    )
    int deleteOldRecords(LocalDate date);

}
