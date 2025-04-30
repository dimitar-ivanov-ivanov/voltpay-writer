package com.voltpay.voltpay_writer.consumer;

import com.voltpay.voltpay_writer.pojo.WriteEvent;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "kafka.dlt.enabled", havingValue = "true")
@AllArgsConstructor
public class DeadLetterConsumer {

    @Autowired
    private final KafkaTemplate<String, WriteEvent> kafkaTemplate;

    /**
     * Consumer for the dead letter topic.
     * It is enabled manually via kafka.dlt.enabled property.
     * Its responsibility is to re-emit the event to the original topic.
     *
     * @param records events to be re-emitted
     */
    @KafkaListener(topics = "write-topic-dlt", containerFactory = "kafkaListenerContainerFactory")
    public void reprocessEvent(List<ConsumerRecord<String, WriteEvent>> records) {
        for(ConsumerRecord<String, WriteEvent> record: records) {
            kafkaTemplate.send("write-topic", record.key(), record.value());
        }
    }
}
