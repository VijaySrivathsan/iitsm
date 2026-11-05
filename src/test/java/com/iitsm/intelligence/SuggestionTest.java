package com.iitsm.intelligence;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.service.IncidentService;
import com.iitsm.support.MutableClock;
import com.iitsm.support.ServiceLayerTest;
import com.iitsm.support.ServiceLayerTestConfig;
import com.iitsm.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optional intelligent features F (category suggestion) and G (suggested resolutions).
 */
@ServiceLayerTest
class SuggestionTest {

    @Autowired private CategorySuggester categorySuggester;
    @Autowired private ResolutionSuggester resolutionSuggester;
    @Autowired private IncidentService incidents;
    @Autowired private TestEntityManager em;
    @Autowired private MutableClock clock;

    private TestData d;

    @BeforeEach
    void setUp() {
        clock.set(ServiceLayerTestConfig.START);
        d = new TestData(em);
    }

    @Test
    void categoryIsSuggestedFromKeywords() {
        assertThat(categorySuggester.suggest("The VPN keeps dropping")).map(Category::getName).contains("NETWORK");
        assertThat(categorySuggester.suggest("Customers cannot complete payment")).map(Category::getName).contains("APPLICATION");
        assertThat(categorySuggester.suggest("The coffee machine is empty")).isEmpty();
        assertThat(categorySuggester.suggest("")).isEmpty();
    }

    @Test
    void similarPastResolutionIsSuggestedForANewIncident() {
        Incident old = incidents.report(d.emp1, d.payment.getId(), d.application.getId(),
                new IncidentReport("UPI payments timing out", "UPI transactions time out at checkout", Impact.HIGH, Urgency.HIGH, 100));
        String n = old.getNumber().toString();
        incidents.acknowledge(n, d.tech1);
        incidents.startWork(n, d.tech1);
        incidents.resolve(n, "Increased connection pool to the UPI provider", "Pool exhausted", d.tech1);

        clock.advance(Duration.ofDays(3));
        Incident fresh = incidents.report(d.emp2, d.payment.getId(), d.application.getId(),
                new IncidentReport("Payments timing out", "Card payment times out at checkout", Impact.HIGH, Urgency.HIGH, 50));
        Incident unrelated = incidents.report(d.emp3, d.vpn.getId(), d.network.getId(),
                new IncidentReport("VPN certificate warning", "Client shows untrusted certificate", Impact.LOW, Urgency.LOW, 1));

        assertThat(resolutionSuggester.suggestFor(fresh)).extracting(ResolutionSuggestion::pastIncident).containsExactly(old);
        assertThat(resolutionSuggester.suggestFor(unrelated)).isEmpty();
    }
}
