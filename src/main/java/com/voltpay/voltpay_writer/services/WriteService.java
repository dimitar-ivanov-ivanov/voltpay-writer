package com.voltpay.voltpay_writer.services;

import com.voltpay.voltpay_writer.entities.*;
import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.repositories.PaymentCoreRepository;
import com.voltpay.voltpay_writer.repositories.PaymentMetadataRepository;
import com.voltpay.voltpay_writer.repositories.PaymentNotesRepository;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import com.voltpay.voltpay_writer.utils.UlidGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class WriteService {

    private static final List<Integer> STATUS_VALUES = Arrays.stream(TrnStatus.values()).map(TrnStatus::getValue).toList();

    private final PaymentCoreRepository paymentCoreRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private final PaymentNotesRepository paymentNotesRepository;

    private final IdempotencyService idempotencyService;

    public void write(Long custId, List<ConsumerRecord<String, WriteEvent>> events, List<ConsumerRecord<String, WriteEvent>> processedEvents) {

        for (ConsumerRecord<String, WriteEvent> event: events) {
            // IF message isn't valid disregard
            String key = event.key();
            WriteEvent value = event.value();

            if (!isEventValid(value)) {
                log.warn("Event with key {} is NOT valid.", key);
                continue;
            }

            Idempotency idempotency = new Idempotency(key, LocalDate.now());
            boolean insertedIdempotency = idempotencyService.insert(idempotency);

            // IF idempotency throws unique constraint we're trying to reprocess a message -> disregard
            if (!insertedIdempotency) {
                return;
            }

            String ulid = UlidGenerator.generateUlid();
            LocalDateTime createdAt = LocalDateTime.now();
            PrimaryKey primaryKey = new PrimaryKey(ulid, createdAt);

            PaymentCore core = PaymentCore.builder()
                    .id(primaryKey)
                    .type(value.getType())
                    .amount(value.getAmount())
                    .custId(custId)
                    .status(value.getStatus())
                    .currency(value.getCurrency())
                    .build();

            PaymentMetadata metadata = PaymentMetadata.builder()
                    .id(primaryKey)
                    .version(1) //TODO: get previous version if its update
                    .updatedAt(LocalDateTime.now()) //TODO: if it's an update on existing record update it
                    .build();

            PaymentNotes note = PaymentNotes.builder()
                    .id(primaryKey)
                    .comment("Create trn") // TODO: think of other comments
                    .build();

            try {
                paymentCoreRepository.save(core);
                paymentMetadataRepository.save(metadata);
                paymentNotesRepository.save(note);
                processedEvents.add(event);
            } catch (Exception ex) {
                log.error("Exception during processing event {}. Sending to Dead Letter.", key);
                // TODO: Send to dead letter
            }
        }
    }

    private boolean isEventValid(WriteEvent event) {
        return event.getAmount() != null &&
                event.getType() != null &&
                event.getCurrency() != null &&
                event.getStatus() != null &&
                event.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                STATUS_VALUES.contains(event.getStatus());
    }
}
