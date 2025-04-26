package com.voltpay.voltpay_writer.services;

import com.voltpay.voltpay_writer.entities.Idempotency;
import com.voltpay.voltpay_writer.repositories.IdempotencyRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    public boolean insert(Idempotency idempotency) {
        try {
            idempotencyRepository.insertNew(idempotency.getId(), idempotency.getDate());
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Idempotency {} already persisted", idempotency.getId());
            return false;
        }
    }
}
