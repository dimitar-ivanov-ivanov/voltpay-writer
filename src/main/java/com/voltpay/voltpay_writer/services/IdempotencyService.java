package com.voltpay.voltpay_writer.services;

import com.voltpay.voltpay_writer.entities.Idempotency;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IdempotencyService {

    @PersistenceContext
    private EntityManager entityManager;

    public boolean insert(Idempotency idempotency) {
        try {
            entityManager.persist(idempotency);
            entityManager.flush(); // force insert now, so exception is thrown immediately
            return true;
        } catch (EntityExistsException ex) {
            log.warn("Idempotency {} already persisted", idempotency.getId());
            return false;
        }
    }
}
