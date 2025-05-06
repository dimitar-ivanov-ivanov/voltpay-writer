package com.voltpay.voltpay_writer.unit.services;

import com.voltpay.voltpay_writer.entities.Idempotency;
import com.voltpay.voltpay_writer.repositories.IdempotencyRepository;
import com.voltpay.voltpay_writer.services.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

        when(repo.findById(idempotency.getId()))
                .thenReturn(Optional.of(idempotency));
        // WHEN
        boolean isInserted = service.insert(idempotency);

        // THEN
        assertFalse(isInserted);
    }

    @Test
    public void given_validInput_when_insert_then_insertSuccessfully() {
        // GIVEN
        Idempotency idempotency = new Idempotency("id2", LocalDate.now());

        when(repo.findById(idempotency.getId()))
            .thenReturn(Optional.empty());

        when(repo.save(idempotency))
                .thenReturn(idempotency);

        // WHEN
        boolean isInserted = service.insert(idempotency);

        // THEN
        assertTrue(isInserted);
    }

    @Test
    public void given_recordToDelete_when_deleteIdempotency_then_delete() {
        // GIVEN
        Idempotency idempotency = new Idempotency("id3", LocalDate.now());
        // WHEN
        service.deleteIdempotency(idempotency);
        // THEN
        verify(repo).delete(idempotency);
    }
}