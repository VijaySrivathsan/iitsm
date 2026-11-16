package com.iitsm.domain.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDependencyTest {

    @Test
    void addAndRemoveDependencies() {
        ITService db = new ITService("Core Database", "PostgreSQL", 4);
        ITService auth = new ITService("Auth / SSO", "SSO", 5);
        ITService payment = new ITService("Payment Gateway", "Payments", 5);

        payment.addDependency(db);
        payment.addDependency(auth);

        assertThat(payment.getDependencies()).containsExactlyInAnyOrder(db, auth);

        payment.removeDependency(db);
        assertThat(payment.getDependencies()).containsExactly(auth);

        payment.clearDependencies();
        assertThat(payment.getDependencies()).isEmpty();
    }

    @Test
    void serviceCannotDependOnItself() {
        ITService service = new ITService("Network", "Switches", 4);
        service.addDependency(service);
        assertThat(service.getDependencies()).isEmpty();
    }
}
