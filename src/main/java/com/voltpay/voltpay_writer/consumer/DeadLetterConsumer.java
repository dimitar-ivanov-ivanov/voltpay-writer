package com.voltpay.voltpay_writer.consumer;

import com.voltpay.voltpay_writer.pojo.WriteEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "kafka.dlt.enabled", havingValue = "true")
public class DeadLetterConsumer {


    @KafkaListener(topics = "write-topic-dlt")
    public void reprocessEvent(List<ConsumerRecord<String, WriteEvent>> records, Acknowledgment acknowledgment) {

    }
}
