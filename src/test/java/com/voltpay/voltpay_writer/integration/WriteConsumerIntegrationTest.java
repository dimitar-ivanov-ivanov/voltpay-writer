package com.voltpay.voltpay_writer.integration;

import com.voltpay.voltpay_writer.entities.Idempotency;
import com.voltpay.voltpay_writer.entities.PaymentCore;
import com.voltpay.voltpay_writer.entities.PaymentMetadata;
import com.voltpay.voltpay_writer.entities.PaymentNotes;
import com.voltpay.voltpay_writer.pojo.WriteEvent;
import com.voltpay.voltpay_writer.repositories.IdempotencyRepository;
import com.voltpay.voltpay_writer.repositories.PaymentCoreRepository;
import com.voltpay.voltpay_writer.repositories.PaymentMetadataRepository;
import com.voltpay.voltpay_writer.repositories.PaymentNotesRepository;
import com.voltpay.voltpay_writer.utils.Currency;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import com.voltpay.voltpay_writer.utils.TrnType;
import com.voltpay.voltpay_writer.utils.UlidGenerator;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SpringBootTest // annotation is needed for autowiring
@ActiveProfiles("test")
public class WriteConsumerIntegrationTest {

    private static final Long CUST_ID = 1L;

    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    private static final String TRN_TYPE = TrnType.BWI.name();

    private static final Integer TRN_STATUS = TrnStatus.SUCCESS.getValue();

    private static final String COMMENT = "Comment";

    private static final String CURRENCY = Currency.EUR.name();

    private static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("write_db")
        .withUsername("user")
        .withPassword("password")
        .withInitScript("init.sql");

    @Autowired
    private KafkaTemplate<String, WriteEvent> kafkaTemplate;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private PaymentCoreRepository paymentCoreRepository;

    @Autowired
    private PaymentMetadataRepository paymentMetadataRepository;

    @Autowired
    private PaymentNotesRepository paymentNotesRepository;

    @BeforeAll
    static void beforeAll() throws NoSuchFieldException, IllegalAccessException {
        kafka.start();
        postgres.start();

        createTopics();
        setNodeId();
    }

    private static void setNodeId() throws NoSuchFieldException, IllegalAccessException {
        Field field = UlidGenerator.class.getDeclaredField("NODE_ID");
        field.setAccessible(true);
        field.set(null, "001");
    }

    private static void createTopics() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic writeTopic = new NewTopic("write-topic", 1, (short) 1);
            NewTopic readTopic = new NewTopic("read-topic", 1, (short) 1);
            NewTopic writeDltTopic = new NewTopic("write-topic-dlt", 1, (short) 1);
            adminClient.createTopics(List.of(writeTopic, readTopic, writeDltTopic)).all().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create topic", e);
        }
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

    @BeforeEach
    void cleanUpDatabase() {
        paymentNotesRepository.deleteAll();
        paymentMetadataRepository.deleteAll();
        paymentCoreRepository.deleteAll();
        idempotencyRepository.deleteAll();
    }

    @Test
    public void given_duplicateIdempotency_when_processBatchOfMessages_then_doNotProcess() {
        // GIVEN
        String messageId = "msg:1";
        WriteEvent writeEvent = getWriteEvent(messageId);

        // WHEN
        kafkaTemplate.send("write-topic", CUST_ID.toString(), writeEvent);
        kafkaTemplate.send("write-topic", CUST_ID.toString(), writeEvent);

        // THEN
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Idempotency> idempotencies = idempotencyRepository.findAll();

                assertEquals(1, idempotencies.size());
                List<PaymentCore> cores = paymentCoreRepository.findAll();
                List<PaymentMetadata> metadataList = paymentMetadataRepository.findAll();
                List<PaymentNotes> notes = paymentNotesRepository.findAll();

                assertEquals(1, cores.size());
                assertEquals(1, metadataList.size());
                assertEquals(1, notes.size());
            });
    }


    @Test
    public void given_validInput_when_processBatchOfMessages_then_processBatch() {
        // GIVEN
        String messageId = "msg:2";
        WriteEvent writeEvent = getWriteEvent(messageId);

        // WHEN
        kafkaTemplate.send("write-topic", CUST_ID.toString(), writeEvent);

        // THEN wait 10 second total to assert, every 2 second we try to do the assertions again
        // CONSIDER increasing timeout when debugging
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS).untilAsserted(() -> {
                Optional<Idempotency> idempotency = idempotencyRepository.findById(messageId);
                assertNotNull(idempotency.get());
                assertEquals(messageId, idempotency.get().getId());

                List<PaymentCore> cores = paymentCoreRepository.findAll();
                List<PaymentMetadata> metadataList = paymentMetadataRepository.findAll();
                List<PaymentNotes> notes = paymentNotesRepository.findAll();

                assertEquals(1, cores.size());
                assertEquals(1, metadataList.size());
                assertEquals(1, notes.size());

                PaymentCore core = cores.stream().findFirst().get();
                PaymentMetadata metadata = metadataList.stream().findFirst().get();
                PaymentNotes note = notes.stream().findFirst().get();

                assertEquals(CUST_ID, core.getCustId());
                assertEquals(AMOUNT.setScale(6), core.getAmount());
                assertEquals(TRN_TYPE, core.getType());
                assertEquals(TRN_STATUS, core.getStatus());

                assertEquals(COMMENT, note.getComment());

                assertEquals(core.getId().getId(), metadata.getId().getId());
                assertEquals(core.getId().getId(), note.getId().getId());
            });
    }

    private static WriteEvent getWriteEvent(String messageId) {
        return WriteEvent.builder()
            .amount(AMOUNT)
            .type(TRN_TYPE)
            .status(TRN_STATUS)
            .comment(COMMENT)
            .messageId(messageId)
            .currency(CURRENCY)
            .custId(CUST_ID)
            .build();
    }
}
