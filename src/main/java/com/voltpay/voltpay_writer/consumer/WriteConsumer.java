package com.voltpay.voltpay_writer.consumer;

import com.voltpay.voltpay_writer.pojo.ReadEvent;
import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.services.WriteService;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class WriteConsumer {

    private static final List<Integer> STATUS_VALUES = Arrays.stream(TrnStatus.values()).map(TrnStatus::getValue).toList();

    private final WriteService writeService;

    private EntityManager entityManager;

    @Autowired
    private final KafkaTemplate<String, ReadEvent> kafkaTemplate;

    @KafkaListener(topics = "write-topic", containerFactory = "kafkaListenerContainerFactory")
    public void processBatchOfMessages(List<ConsumerRecord<String, WriteEvent>> records, Acknowledgment ack) {
        // map events by customer id
        Map<Long, List<ConsumerRecord<String, WriteEvent>>> map = records.stream()
                .filter(this::isEventValid)
                .collect(Collectors.groupingBy(record -> record.value().getCustId()));

        if (map.isEmpty()) {
            // TODO: See if it's OK to commit automatically even we have failed events in the batch
            ack.acknowledge();
            return;
        }

        List<ReadEvent> successfulEvents = new ArrayList<>();
        // Not allowed to create transaction on shared EntityManager - use Spring transactions or EJB CMT instead
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();
            for (Long custId : map.keySet()) {
                List<ConsumerRecord<String, WriteEvent>> events = map.get(custId);
                writeService.write(custId, events, successfulEvents);
            }
            transaction.commit();
            publishEventsToReadTopic(successfulEvents);
        } catch (Exception ex) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }

        // We commit once for the whole batch
        // TODO: See if it's OK to commit automatically even we have failed events in the batch
        ack.acknowledge();
    }

    private void publishEventsToReadTopic(List<ReadEvent> successfulEvents) {
        for (ReadEvent event: successfulEvents) {
            kafkaTemplate.send("read-topic", event);
        }
    }

    private boolean isEventValid(ConsumerRecord<String, WriteEvent> record) {
        String key = record.key();
        Object val = record.value();

        if (key == null || val.getClass() != WriteEvent.class) {
            return false;
        }

        WriteEvent event = (WriteEvent) val;

        return event.getAmount() != null &&
                event.getType() != null &&
                event.getCurrency() != null &&
                event.getStatus() != null &&
                event.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                STATUS_VALUES.contains(event.getStatus());
    }
}
