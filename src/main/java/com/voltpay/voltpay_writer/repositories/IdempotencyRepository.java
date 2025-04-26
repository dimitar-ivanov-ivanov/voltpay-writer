package com.voltpay.voltpay_writer.repositories;

import com.voltpay.voltpay_writer.entities.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface IdempotencyRepository extends JpaRepository<Idempotency, String> {

  @Modifying
  @Query(
          value = "INSERT INTO write.idempotency (id, date) VALUES (:id, :date)",
          nativeQuery = true
  )
  int insertNew(@Param("id") String id, @Param("date")LocalDate date);
}
