package com.voltpay.voltpay_writer.services;

import com.voltpay.voltpay_writer.entities.Idempotency;
import com.voltpay.voltpay_writer.entities.PaymentCore;
import com.voltpay.voltpay_writer.entities.PaymentMetadata;
import com.voltpay.voltpay_writer.entities.PaymentNotes;
import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.repositories.PaymentCoreRepository;
import com.voltpay.voltpay_writer.repositories.PaymentMetadataRepository;
import com.voltpay.voltpay_writer.repositories.PaymentNotesRepository;
import com.voltpay.voltpay_writer.utils.Currency;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import com.voltpay.voltpay_writer.utils.TrnType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WriteServiceTest {

    private static final String TOPIC = "write-topic";

    private static final Long CUST_ID = 1L;

    private PaymentCoreRepository paymentCoreRepository;

    private PaymentMetadataRepository paymentMetadataRepository;

    private PaymentNotesRepository paymentNotesRepository;

    private IdempotencyService idempotencyService;

    private KafkaTemplate<String, WriteEvent> deadLetterTemplate;

    private WriteService service;

    @BeforeEach
    public void setUp() {
        paymentCoreRepository = mock(PaymentCoreRepository.class);
        paymentMetadataRepository = mock(PaymentMetadataRepository.class);
        paymentNotesRepository = mock(PaymentNotesRepository.class);
        idempotencyService = mock(IdempotencyService.class);
        deadLetterTemplate = mock(KafkaTemplate.class);

        service = new WriteService(paymentCoreRepository, paymentMetadataRepository,
            paymentNotesRepository, idempotencyService, deadLetterTemplate);
    }

    @Test
    public void given_failedIdempotency_when_write_then_doNoWrite() {
        // GIVEN
        ConsumerRecord<String, WriteEvent> record = new ConsumerRecord<>(TOPIC, 1, 1, "k1", new WriteEvent());
        when(idempotencyService.insert(any(Idempotency.class))).thenReturn(false);

        // WHEN
        service.write(CUST_ID, List.of(record), new ArrayList<>());

        // THEN
        verifyNoInteractions(deadLetterTemplate);
        verifyNoInteractions(paymentCoreRepository);
        verifyNoInteractions(paymentMetadataRepository);
        verifyNoInteractions(paymentNotesRepository);
    }

    @Test
    public void given_exceptionWhenWriting_when_write_then_sendToDeadLetter() {
        // GIVEN
        String key = "k1";
        WriteEvent value = new WriteEvent();
        ConsumerRecord<String, WriteEvent> record = new ConsumerRecord<>(TOPIC, 1, 1, key, value);
        when(idempotencyService.insert(any(Idempotency.class))).thenReturn(true);
        when(paymentCoreRepository.save(any(PaymentCore.class))).thenThrow(IllegalArgumentException.class);

        // WHEN
        service.write(CUST_ID, List.of(record), new ArrayList<>());

        // THEN
        verifyNoInteractions(paymentMetadataRepository);
        verifyNoInteractions(paymentNotesRepository);
        verify(deadLetterTemplate).send("write-topic-dlt", key, value);
    }

    @Test
    public void given_validInput_write_then_writeSuccessfully() {
        // GIVEN
        String key = "k1";
        WriteEvent value = WriteEvent.builder()
            .custId(CUST_ID)
            .type(TrnType.BWI.name())
            .amount(BigDecimal.TEN)
            .status(TrnStatus.PENDING.getValue())
            .comment("Comment")
            .messageId("messageId")
            .currency(Currency.EUR.name())
            .build();
        ConsumerRecord<String, WriteEvent> record = new ConsumerRecord<>(TOPIC, 1, 1, key, value);
        when(idempotencyService.insert(any(Idempotency.class))).thenReturn(true);

        // WHEN
        service.write(CUST_ID, List.of(record), new ArrayList<>());

        // THEN
        ArgumentCaptor<PaymentCore> paymentCoreArgumentCaptor = ArgumentCaptor.forClass(PaymentCore.class);
        ArgumentCaptor<PaymentMetadata> paymentMetadataArgumentCaptor = ArgumentCaptor.forClass(PaymentMetadata.class);
        ArgumentCaptor<PaymentNotes> paymentNotesArgumentCaptor = ArgumentCaptor.forClass(PaymentNotes.class);

        verify(paymentCoreRepository).save(paymentCoreArgumentCaptor.capture());
        verify(paymentMetadataRepository).save(paymentMetadataArgumentCaptor.capture());
        verify(paymentNotesRepository).save(paymentNotesArgumentCaptor.capture());

        PaymentCore paymentCore = paymentCoreArgumentCaptor.getValue();
        assertEquals(value.getCustId(), paymentCore.getCustId());
        assertEquals(value.getAmount(), paymentCore.getAmount());
        assertEquals(value.getType(), paymentCore.getType());
        assertEquals(value.getStatus(), paymentCore.getStatus());
        assertEquals(value.getCurrency(), paymentCore.getCurrency());

        PaymentNotes paymentNotes = paymentNotesArgumentCaptor.getValue();
        assertEquals(value.getComment(), paymentNotes.getComment());

        PaymentMetadata paymentMetadata = paymentMetadataArgumentCaptor.getValue();
        assertEquals(paymentCore.getId(), paymentMetadata.getId());
        assertEquals(paymentCore.getId(), paymentNotes.getId());

        verifyNoInteractions(deadLetterTemplate);
    }
}
