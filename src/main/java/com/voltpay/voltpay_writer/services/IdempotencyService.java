package com.voltpay.voltpay_writer.services;

import com.voltpay.voltpay_writer.entities.Idempotency;
import com.voltpay.voltpay_writer.repositories.IdempotencyRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    /**
     * Try to insert idempotency record.
     * Explicitly check if record exists, this is done to avoid throwing exceptions.
     * If an exception is thrown is will make the entire transaction for the batch rollback.
     * Additionally, we have catch block for duplicate record in case we have some race conditions.
     *
     * This method reuses the transaction that is started in {@link com.voltpay.voltpay_writer.consumer.WriteConsumer}
     * @param idempotency record to persist
     * @return boolean result indicating if record was persisted
     */
    @Transactional
    public boolean insert(Idempotency idempotency) {
        if (idempotencyRepository.findById(idempotency.getId()).isPresent()) {
            return false;
        }
        try {
            idempotencyRepository.save(idempotency);
            return true;
        }  catch (DataIntegrityViolationException ex) {
            log.warn("Idempotency {} already persisted", idempotency.getId());
            return false;
        }
    }

    public void deleteIdempotency(Idempotency idempotency) {
        try {
            idempotencyRepository.delete(idempotency);
        } catch (Exception ex) {
            log.warn("Couldn't delete idempotency {}. It has to be manually deleted.", idempotency.getId());
        }
    }
}
