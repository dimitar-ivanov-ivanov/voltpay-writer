package com.voltpay.voltpay_writer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumer {

    public void processBatchOfMessages() {
        // idempotency hit, this is in the SAME TRANSACTION as writing to the DB

        // IF idempotency throws unique constraint we're trying to reprocess a message -> disregard

        // do business logic

        // if message isn't valid put

        // We commit once for the whole batch


    }
}
