package com.voltpay.voltpay_writer.consumer;

import com.voltpay.voltpay_writer.pojo.WriteEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "kafka.dlt.enabled", havingValue = "true")
@AllArgsConstructor
@Slf4j
public class DeadLetterConsumer {

    @Autowired
    private final KafkaTemplate<String, WriteEvent> kafkaTemplate;

    /**
     * Consumer for the dead letter topic.
     * It is enabled manually via kafka.dlt.enabled property.
     * Its responsibility is to re-emit the event to the original topic.
     *
     * @param events events to be re-emitted
     */
    @KafkaListener(topics = "write-topic-dlt", containerFactory = "deadLetterListenerContainerFactory")
    public void reprocessEvent(List<WriteEvent> events) {
        for(WriteEvent event: events) {
            log.info("Attempting to produce message {} to write-topic for reprocessing.", event.getMessageId());
            kafkaTemplate.send("write-topic", event.getCustId().toString(), event);
            log.info("Successfully produced message {}", event.getMessageId());
        }
    }
}
