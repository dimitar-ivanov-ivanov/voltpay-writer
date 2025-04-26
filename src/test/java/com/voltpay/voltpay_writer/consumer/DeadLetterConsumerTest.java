package com.voltpay.voltpay_writer.consumer;

import com.voltpay.voltpay_writer.pojo.WriteEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeadLetterConsumerTest {

    private static final String INPUT_TOPIC = "write-topic-dlt";

    private static final String OUTPUT_TOPIC = "write-topic";

    private KafkaTemplate<String, WriteEvent> kafkaTemplate;

    private DeadLetterConsumer consumer;

    @BeforeEach
    public void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        consumer = new DeadLetterConsumer(kafkaTemplate);
    }

    @Test
    public void given_eventToReprocess_when_reprocessEvent_then_reprocessSuccessfully() {
        // GIVEN
        String key = "key";
        WriteEvent value = new WriteEvent();
        ConsumerRecord<String, WriteEvent> record = new ConsumerRecord<>(INPUT_TOPIC, 1, 1, key, value);
        // WHEN
        consumer.reprocessEvent(List.of(record));
        // THEN
        verify(kafkaTemplate).send(OUTPUT_TOPIC, key, value);
    }
}