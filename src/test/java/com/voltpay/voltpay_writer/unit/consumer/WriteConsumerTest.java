package com.voltpay.voltpay_writer.unit.consumer;

import com.voltpay.voltpay_writer.consumer.WriteConsumer;
import com.voltpay.voltpay_writer.pojo.ReadEvent;
import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.services.WriteService;
import com.voltpay.voltpay_writer.utils.Currency;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import com.voltpay.voltpay_writer.utils.TrnType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WriteConsumerTest {

    private static final String TOPIC = "write-topic";

    private static final Long CUST_ID = 1L;

    private WriteService writeService;

    private KafkaTemplate<String, ReadEvent> readEventKafkaTemplate;

    private PlatformTransactionManager transactionManager;

    private WriteConsumer consumer;

    @BeforeEach
    public void setUp() {
        writeService = mock(WriteService.class);
        readEventKafkaTemplate = mock(KafkaTemplate.class);
        transactionManager = mock(PlatformTransactionManager.class);
        consumer = new WriteConsumer(writeService, readEventKafkaTemplate, transactionManager);
    }

    @ParameterizedTest
    @MethodSource("invalidRecords")
    public void given_invalidEvent_when_processBatchOfMessages_then_doNotProcess(String key, WriteEvent value) {
        // GIVEN
        ConsumerRecord<String, WriteEvent> record = new ConsumerRecord<>(TOPIC, 1, 0, key, value);

        // WHEN
        consumer.processBatchOfMessages(List.of(record));

        // THEN
        verifyNoInteractions(writeService);
        verifyNoInteractions(transactionManager);
        verifyNoInteractions(readEventKafkaTemplate);
    }

    @Test
    public void given_noSuccessfulEventsWritten_when_processBatchOfMessages_then_rollbackTransaction() {
        // GIVEN
        String key = "k1";
        WriteEvent value = WriteEvent.builder().amount(BigDecimal.ONE)
            .type(TrnType.BWI.name())
            .currency(Currency.EUR.name())
            .status(TrnStatus.SUCCESS.getValue())
            .custId(CUST_ID)
            .build();

        ConsumerRecord<String, WriteEvent> record = new ConsumerRecord<>(TOPIC, 1, 0, key, value);

        // WHEN
        consumer.processBatchOfMessages(List.of(record));

        // THEN
        verifyNoInteractions(readEventKafkaTemplate);
        verify(transactionManager).rollback(any());
    }

    @Test
    public void given_successfullyWrittenEvents_when_processBatchOfMessages_then_publishEventsToReadTopic() {
        // GIVEN
        String key = "k1";
        WriteEvent value = WriteEvent.builder().amount(BigDecimal.ONE)
            .type(TrnType.BWI.name())
            .currency(Currency.EUR.name())
            .status(TrnStatus.SUCCESS.getValue())
            .custId(CUST_ID)
            .build();

        ConsumerRecord<String, WriteEvent> record = new ConsumerRecord<>(TOPIC, 1, 0, key, value);

        String messageId = "1312312";
        ReadEvent readEvent = new ReadEvent();
        readEvent.setMessageId(messageId);

        doAnswer(invocation -> {
            List<ReadEvent> processedEvents = invocation.getArgument(2);
            processedEvents.add(readEvent);
            return null;
        }).when(writeService).write(anyLong(), anyList(), anyList());

        // WHEN
        consumer.processBatchOfMessages(List.of(record));

        // THEN
        verify(readEventKafkaTemplate).send("read-topic", messageId, readEvent);
        verify(transactionManager).commit(any());
    }

    private static Stream<Arguments> invalidRecords() {
        return Stream.of(
            // null key
            Arguments.of(null, new WriteEvent()),
            // null value
            Arguments.of("k1", null),
            // no amount in event
            Arguments.of("k1", new WriteEvent()),
            // no transaction type in event
            Arguments.of("k1", WriteEvent.builder().amount(BigDecimal.ONE).build()),
            // no currency
            Arguments.of("k1", WriteEvent.builder().amount(BigDecimal.ONE).type(TrnType.BWI.name()).build()),
            // no status
            Arguments.of("k1", WriteEvent.builder().amount(BigDecimal.ONE)
                .type(TrnType.BWI.name())
                .currency(Currency.EUR.name())
                .build()),
            // invalid status
            Arguments.of("k1", WriteEvent.builder().amount(BigDecimal.ONE)
                .type(TrnType.BWI.name())
                .currency(Currency.EUR.name())
                .status(-100)
                .custId(CUST_ID)
                .build()),
            // null cust id
            Arguments.of("k1", WriteEvent.builder().amount(BigDecimal.ONE)
                .type(TrnType.BWI.name())
                .currency(Currency.EUR.name())
                .status(TrnStatus.PENDING.getValue())
                .build()),
            // zero amount
            Arguments.of("k1", WriteEvent.builder().amount(BigDecimal.ZERO)
                .type(TrnType.BWI.name())
                .currency(Currency.EUR.name())
                .status(TrnStatus.SUCCESS.getValue())
                .custId(CUST_ID)
                .build()),
            // negative amount
            Arguments.of("k1", WriteEvent.builder().amount(BigDecimal.ZERO)
                .type(TrnType.BWI.name())
                .currency(Currency.EUR.name())
                .status(TrnStatus.SUCCESS.getValue())
                .custId(CUST_ID)
                .build())
        );
    }
}