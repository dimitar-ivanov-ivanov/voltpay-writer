package com.voltpay.voltpay_writer.unit.services;

import com.voltpay.voltpay_writer.entities.Idempotency;
import com.voltpay.voltpay_writer.repositories.IdempotencyRepository;
import com.voltpay.voltpay_writer.services.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    private IdempotencyRepository repo;

    private IdempotencyService service;

    @BeforeEach
    public void setUp() {
        repo = mock(IdempotencyRepository.class);
        service = new IdempotencyService(repo);
    }

    @Test
    public void given_duplicateIdempotency_when_insert_then_returnFalse() {
        // GIVEN
        Idempotency idempotency = new Idempotency("id", LocalDate.now());

        when(repo.insertNew(idempotency.getId(), idempotency.getDate()))
                .thenThrow(DataIntegrityViolationException.class);
        // WHEN
        boolean isInserted = service.insert(idempotency);
        // THEN
        assertFalse(isInserted);
    }

    @Test
    public void given_validInput_when_insert_then_insertSuccessfully() {
        // GIVEN
        Idempotency idempotency = new Idempotency("id", LocalDate.now());

        when(repo.insertNew(idempotency.getId(), idempotency.getDate()))
                .thenReturn(1);

        // WHEN
        boolean isInserted = service.insert(idempotency);

        // THEN
        assertTrue(isInserted);
    }
}