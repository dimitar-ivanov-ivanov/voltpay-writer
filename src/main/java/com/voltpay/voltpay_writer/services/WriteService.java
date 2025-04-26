package com.voltpay.voltpay_writer.services;

import com.voltpay.voltpay_writer.entities.Idempotency;
import com.voltpay.voltpay_writer.entities.PaymentCore;
import com.voltpay.voltpay_writer.entities.PaymentMetadata;
import com.voltpay.voltpay_writer.entities.PaymentNotes;
import com.voltpay.voltpay_writer.entities.PrimaryKey;
import com.voltpay.voltpay_writer.pojo.ReadEvent;
import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.repositories.PaymentCoreRepository;
import com.voltpay.voltpay_writer.repositories.PaymentMetadataRepository;
import com.voltpay.voltpay_writer.repositories.PaymentNotesRepository;
import com.voltpay.voltpay_writer.utils.UlidGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class WriteService {

    private final PaymentCoreRepository paymentCoreRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private final PaymentNotesRepository paymentNotesRepository;

    private final IdempotencyService idempotencyService;

    @Autowired
    private final KafkaTemplate<String, WriteEvent> deadLetterTemplate;

    public void write(Long custId, List<ConsumerRecord<String, WriteEvent>> events, List<ReadEvent> processedEvents) {

        for (ConsumerRecord<String, WriteEvent> event: events) {
            String key = event.key();
            WriteEvent value = event.value();
            String messageId = value.getMessageId();

            Idempotency idempotency = new Idempotency(messageId, LocalDate.now());
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

            Integer version = 1; //TODO: get previous version if its update

            PaymentMetadata metadata = PaymentMetadata.builder()
                    .id(primaryKey)
                    .version(version)
                    .updatedAt(createdAt) //TODO: if it's an update on existing record update it
                    .build();

            PaymentNotes note = PaymentNotes.builder()
                    .id(primaryKey)
                    .comment(value.getComment())
                    .build();

            try {
                paymentCoreRepository.save(core);
                paymentMetadataRepository.save(metadata);
                paymentNotesRepository.save(note);

                ReadEvent readEvent = ReadEvent.builder()
                        .id(ulid)
                        .messageId(messageId)
                        .createdAt(createdAt)
                        .updatedAt(createdAt)
                        .version(version)
                        .type(value.getType())
                        .status(value.getStatus())
                        .custId(custId)
                        .comment(value.getComment())
                        .amount(value.getAmount())
                        .build();

                processedEvents.add(readEvent);
            } catch (Exception ex) {
                log.error("Exception during processing event {}. Sending to Dead Letter.", messageId);
                sendToDeadLetter(key, value);
            }
        }
    }

    private void sendToDeadLetter(String key, WriteEvent value) {
        try {
            deadLetterTemplate.send("write-topic-dlt", key, value);
        } catch (Exception ex) {
            log.error("Error while trying to send to Dead letter.", ex);
        }
    }
}
