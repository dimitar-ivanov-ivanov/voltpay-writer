package com.voltpay.voltpay_writer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VoltpayWriterApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoltpayWriterApplication.class, args);
    }
}
