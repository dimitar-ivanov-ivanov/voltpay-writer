package com.voltpay.voltpay_writer.consumer;

import com.voltpay.voltpay_writer.pojo.ReadEvent;
import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.services.WriteService;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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

    @Autowired
    private final KafkaTemplate<String, ReadEvent> readEventKafkaTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @KafkaListener(topics = "write-topic", containerFactory = "kafkaListenerContainerFactory")
    public void processBatchOfMessages(List<ConsumerRecord<String, WriteEvent>> records) {
        // map events by customer id
        Map<Long, List<ConsumerRecord<String, WriteEvent>>> map = records.stream()
                .filter(this::isEventValid)
                .collect(Collectors.groupingBy(record -> record.value().getCustId()));

        if (map.isEmpty()) {
            return;
        }

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("batch-write-transaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);

        List<ReadEvent> successfulEvents = new ArrayList<>();

        try {
            for(Long custId: map.keySet()) {
                List<ConsumerRecord<String, WriteEvent>> events = map.get(custId);
                writeService.write(custId, events, successfulEvents);
            }

            if (!successfulEvents.isEmpty()) {
                transactionManager.commit(status);
                publishEventsToReadTopic(successfulEvents);
            } else {
                transactionManager.rollback(status);
            }
        } catch (Exception ex) {
            log.error("Failed to process events", ex);
        }
    }

    private void publishEventsToReadTopic(List<ReadEvent> successfulEvents) {
        for (ReadEvent event: successfulEvents) {
            readEventKafkaTemplate.send("read-topic", event.getMessageId(), event);
        }
    }

    private boolean isEventValid(ConsumerRecord<String, WriteEvent> record) {
        String key = record.key();
        Object val = record.value();

        // Explicitly checking for warmup events
        if (key == null || val == null || val.getClass() != WriteEvent.class) {
            return false;
        }

        WriteEvent event = (WriteEvent) val;

        return event.getAmount() != null &&
                event.getType() != null &&
                event.getCurrency() != null &&
                event.getStatus() != null &&
                event.getCustId() != null &&
                event.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                STATUS_VALUES.contains(event.getStatus());
    }
}
