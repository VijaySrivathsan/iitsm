package com.iitsm.notification;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.exception.ExternalServiceException;
import com.iitsm.repository.NotificationRepository;
import com.iitsm.service.IncidentService;
import com.iitsm.support.ServiceLayerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * If the SMS provider is down, the incident is still created and assigned, and the other channels
 * still deliver.
 */
@ServiceLayerTest
@Import(FailingSmsGatewayTest.BrokenSms.class)
class FailingSmsGatewayTest {

    @TestConfiguration
    static class BrokenSms {
        @Bean
        @Primary
        SmsGateway brokenSmsGateway() {
            return new SmsGateway() {
                @Override
                public void send(String phoneNumber, String text) {
                    throw new ExternalServiceException("SMS provider unreachable");
                }

                @Override
                public List<OutboxMessage> recentlySent() {
                    return List.of();
                }
            };
        }
    }

    @Autowired private IncidentService incidents;
    @Autowired private NotificationRepository notifications;
    @Autowired private TestEntityManager em;

    @Test
    void smsFailure_doesNotBreakIncidentOrOtherChannels() {
        Employee emp = em.persist(new Employee("emp1", "h", "Asha", null, null, null));
        Category application = em.persist(new Category("Application", null, null));
        Technician tech = new Technician("tech1", "h", "Karan", "karan@corp.test", "+910000000002", 5, 4);
        tech.addSkill(application);
        em.persist(tech);
        ITService payment = em.persist(new ITService("Payment Gateway", null, 5));

        Incident incident = incidents.report(emp, payment.getId(), application.getId(),
                new IncidentReport("Payment down", "Cannot pay", Impact.HIGH, Urgency.HIGH, 500));

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(notifications.findByRecipientAndChannelOrderByCreatedAtDesc(tech, ChannelType.IN_APP)).isNotEmpty();
        assertThat(notifications.findByRecipientAndChannelOrderByCreatedAtDesc(tech, ChannelType.EMAIL)).isNotEmpty();
        assertThat(notifications.findByRecipientAndChannelOrderByCreatedAtDesc(tech, ChannelType.SMS)).isEmpty();
    }
}
