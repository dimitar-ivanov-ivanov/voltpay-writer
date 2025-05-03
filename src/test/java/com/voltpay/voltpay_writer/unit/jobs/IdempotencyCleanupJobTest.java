package com.voltpay.voltpay_writer.unit.jobs;

import com.voltpay.voltpay_writer.jobs.IdempotencyCleanupJob;
import com.voltpay.voltpay_writer.repositories.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdempotencyCleanupJobTest {

    private IdempotencyRepository idempotencyRepository;

    private IdempotencyCleanupJob job;

    @BeforeEach
    public void setUp() {
        idempotencyRepository = mock(IdempotencyRepository.class);
        job = new IdempotencyCleanupJob(idempotencyRepository);
    }

    @Test
    public void given_validInput_when_cleanUp_then_successfullyCleanUp() {
        // GIVEN
        int deletedRecords = 10;
        when(idempotencyRepository.deleteOldRecords(any(LocalDate.class))).thenReturn(deletedRecords);
        // WHEN
        int result = job.cleanUp();
        // THEN
        assertEquals(deletedRecords, result);
    }
}