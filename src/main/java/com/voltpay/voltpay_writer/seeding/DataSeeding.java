package com.voltpay.voltpay_writer.seeding;

import com.voltpay.voltpay_writer.entities.PaymentCore;
import com.voltpay.voltpay_writer.entities.PaymentMetadata;
import com.voltpay.voltpay_writer.entities.PaymentNotes;
import com.voltpay.voltpay_writer.entities.PrimaryKey;
import com.voltpay.voltpay_writer.repositories.PaymentCoreRepository;
import com.voltpay.voltpay_writer.repositories.PaymentMetadataRepository;
import com.voltpay.voltpay_writer.repositories.PaymentNotesRepository;
import com.voltpay.voltpay_writer.utils.Currency;
import com.voltpay.voltpay_writer.utils.TrnStatus;
import com.voltpay.voltpay_writer.utils.TrnType;
import com.voltpay.voltpay_writer.utils.UlidGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DataSeeding implements CommandLineRunner {

    @Autowired
    private PaymentCoreRepository paymentCoreRepository;

    @Autowired
    private PaymentMetadataRepository paymentMetadataRepository;

    @Autowired
    private PaymentNotesRepository paymentNotesRepository;

    @Override
    public void run(String... args) throws Exception {

        for (int i = 0; i < 10; i++) {
            String ulid = UlidGenerator.generateUlid();
            PrimaryKey key = new PrimaryKey(ulid, LocalDateTime.now());

            PaymentCore paymentCore = new PaymentCore();
            paymentCore.setAmount(new BigDecimal("10.11133333333333333"));
            paymentCore.setCurrency(Currency.EUR.name());
            paymentCore.setType(TrnType.BWI.name());
            paymentCore.setCustId(2L);
            paymentCore.setStatus(TrnStatus.SUCCESS.getValue());
            paymentCore.setId(key);


            PaymentMetadata paymentMetadata = new PaymentMetadata();
            paymentMetadata.setId(key);
            paymentMetadata.setVersion(1);

            PaymentNotes paymentNotes = new PaymentNotes();
            paymentNotes.setId(key);
            paymentNotes.setComment("Comment");

            paymentCoreRepository.save(paymentCore);
            paymentMetadataRepository.save(paymentMetadata);
            paymentNotesRepository.save(paymentNotes);

        }
    }
}
