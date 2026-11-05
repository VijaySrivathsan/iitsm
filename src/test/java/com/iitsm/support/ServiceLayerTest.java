package com.iitsm.support;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Integration test of the service layer against an in-memory H2 database, with all services,
 * strategies and listeners loaded and published events recorded. Each test runs in a transaction
 * that is rolled back afterwards.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@Import(ServiceLayerTestConfig.class)
@RecordApplicationEvents
public @interface ServiceLayerTest {
}
