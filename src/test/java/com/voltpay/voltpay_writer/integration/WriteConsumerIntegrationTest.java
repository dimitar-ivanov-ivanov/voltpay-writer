package com.voltpay.voltpay_writer.integration;

import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.utils.Currency;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import com.voltpay.voltpay_writer.utils.TrnType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

@SpringBootTest
public class WriteConsumerIntegrationTest {

    private static final Long CUST_ID = 1L;

    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    private static final String TRN_TYPE = TrnType.BWI.name();

    private static final int TRN_STATUS = TrnStatus.SUCCESS.getValue();

    private static final String COMMENT = "Comment";

    private static final String MESSAGE_ID = "msgId";

    private static final String CURRENCY = Currency.EUR.name();

    private static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private KafkaTemplate<String, WriteEvent> kafkaTemplate;

    @BeforeAll
    static void beforeAll() {
        kafka.start();
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        kafka.stop();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Postgres config
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Kafka config
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.max-poll-records", () -> 250);
        registry.add("spring.kafka.consumer.group-id", () -> "writer-group");
        registry.add("spring.kafka.consumer.fetch-max-wait", () -> 500);
    }

    @Test
    public void given_validInput_when_processBatchOfMessages_then_processBatch() {
        // GIVEN
        WriteEvent writeEvent = getWriteEvent();

        // WHEN
        kafkaTemplate.send("write-topic", CUST_ID.toString(), writeEvent);

        // THEN
    }

    private static WriteEvent getWriteEvent() {
        return WriteEvent.builder()
            .amount(AMOUNT)
            .type(TRN_TYPE)
            .status(TRN_STATUS)
            .comment(COMMENT)
            .messageId(MESSAGE_ID)
            .currency(CURRENCY)
            .custId(CUST_ID)
            .build();
    }
}
