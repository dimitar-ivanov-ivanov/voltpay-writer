package com.voltpay.voltpay_writer.consumer;

import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.services.WriteService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaConsumer {

    private final WriteService writeService;

    private EntityManager entityManager;

    @KafkaListener(topics = "write-topic", containerFactory = "kafkaListenerContainerFactory")
    public void processBatchOfMessages(List<ConsumerRecord<String, WriteEvent>> records, Acknowledgment ack) {
        // map events by customer id
        Map<Long, List<ConsumerRecord<String, WriteEvent>>> map = records.stream()
                .collect(Collectors.groupingBy(record -> record.value().getCustId()));

        List<ConsumerRecord<String, WriteEvent>> successfulEvents = new ArrayList<>();
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();
            for (Long custId : map.keySet()) {
                List<ConsumerRecord<String, WriteEvent>> events = map.get(custId);
                writeService.write(custId, events, successfulEvents);
            }
            transaction.commit();
            // TODO: Send successfulEvents to new topic
        } catch (Exception ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }

        // We commit once for the whole batch
        // TODO: See if it's OK to commit automatically even we have failed events in the batch
        ack.acknowledge();
    }
}
