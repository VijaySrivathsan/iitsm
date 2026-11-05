package com.iitsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the I-ITSM application. Spring Boot scans {@code com.iitsm} and its sub-packages.
 */
@SpringBootApplication
public class ItsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItsmApplication.class, args);
    }
}
