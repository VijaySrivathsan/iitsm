package com.iitsm.notification;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Impact;
import com.iitsm.domain.incident.Incident;
import com.iitsm.domain.incident.IncidentReport;
import com.iitsm.domain.incident.IncidentStatus;
import com.iitsm.domain.incident.Urgency;
import com.iitsm.domain.notification.ChannelType;
import com.iitsm.domain.notification.Notification;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.repository.NotificationRepository;
import com.iitsm.service.IncidentService;
import com.iitsm.support.ServiceLayerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Who gets told what, through which channel (Observer + Strategy + Adapter working together).
 */
@ServiceLayerTest
class NotificationTest {

    @Autowired private IncidentService incidents;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notifications;
    @Autowired private EmailGateway emailGateway;
    @Autowired private SmsGateway smsGateway;
    @Autowired private TestEntityManager em;

    private Employee emp;
    private Technician tech;
    private Administrator admin;
    private ITService payment;
    private ITService hrPortal;
    private Category application;

    @BeforeEach
    void setUp() {
        emp = em.persist(new Employee("emp1", "h", "Asha", "asha@corp.test", "+910000000001", null));
        admin = em.persist(new Administrator("admin", "h", "Anita", "anita@corp.test", "+910000000003", true));
        application = em.persist(new Category("Application", null, null));
        tech = new Technician("tech1", "h", "Karan", "karan@corp.test", "+910000000002", 5, 4);
        tech.addSkill(application);
        em.persist(tech);
        payment = em.persist(new ITService("Payment Gateway", null, 5));
        hrPortal = em.persist(new ITService("HR Portal", null, 2));
    }

    private Incident report(ITService service, Impact impact, Urgency urgency, int users) {
        return incidents.report(emp, service.getId(), application.getId(),
                new IncidentReport("Something broke", "Details", impact, urgency, users));
    }

    private List<Notification> sentTo(User user, ChannelType channel) {
        return notifications.findByRecipientAndChannelOrderByCreatedAtDesc(user, channel);
    }

    @Test
    void p1Assignment_reachesTechnicianByInAppEmailAndSms() {
        Incident incident = report(payment, Impact.HIGH, Urgency.HIGH, 500); // P1, auto-assigned to tech1

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(sentTo(tech, ChannelType.IN_APP)).extracting(Notification::getSubject)
                .anyMatch(s -> s.contains("assigned to Karan") && s.contains("P1"));
        assertThat(sentTo(tech, ChannelType.EMAIL)).hasSize(1);
        assertThat(sentTo(tech, ChannelType.SMS)).hasSize(1);
        assertThat(emailGateway.recentlySent()).anyMatch(m -> m.to().equals("karan@corp.test"));
        assertThat(smsGateway.recentlySent()).anyMatch(m -> m.to().equals("+910000000002"));
    }

    @Test
    void lowPriorityAssignment_doesNotSendSms() {
        report(hrPortal, Impact.LOW, Urgency.LOW, 1); // P4
        assertThat(sentTo(tech, ChannelType.IN_APP)).isNotEmpty();
        assertThat(sentTo(tech, ChannelType.SMS)).isEmpty();
    }

    @Test
    void employees_neverGetSms_evenWithAPhoneNumber() {
        report(payment, Impact.HIGH, Urgency.HIGH, 500);
        assertThat(sentTo(emp, ChannelType.IN_APP)).isNotEmpty();
        assertThat(sentTo(emp, ChannelType.SMS)).isEmpty();
    }

    @Test
    void actorIsNotNotifiedOfTheirOwnAction() {
        String n = report(payment, Impact.HIGH, Urgency.HIGH, 500).getNumber().toString();
        int techBefore = sentTo(tech, ChannelType.IN_APP).size();

        incidents.acknowledge(n, tech);

        assertThat(sentTo(tech, ChannelType.IN_APP)).hasSize(techBefore);
        assertThat(sentTo(emp, ChannelType.IN_APP)).extracting(Notification::getSubject)
                .anyMatch(s -> s.contains("acknowledged"));
        // the reporter did not get a "received" message for the incident they created themselves
        assertThat(sentTo(emp, ChannelType.IN_APP)).noneMatch(x -> x.getSubject().contains("received"));
    }

    @Test
    void noTechnicianAvailable_alertsAdministrators() {
        tech.markUnavailable();
        report(payment, Impact.HIGH, Urgency.HIGH, 500);
        assertThat(sentTo(admin, ChannelType.IN_APP)).extracting(Notification::getSubject)
                .anyMatch(s -> s.contains("no technician available"));
        assertThat(sentTo(admin, ChannelType.SMS)).isNotEmpty(); // urgent
    }

    @Test
    void inbox_unreadCountAndMarkRead() {
        report(payment, Impact.HIGH, Urgency.HIGH, 500);
        long unread = notificationService.unreadCount(tech);
        assertThat(unread).isPositive();

        Notification first = notificationService.inbox(tech).get(0);
        notificationService.markRead(first.getId(), tech);
        assertThat(notificationService.unreadCount(tech)).isEqualTo(unread - 1);

        assertThatThrownBy(() -> notificationService.markRead(first.getId(), emp))
                .isInstanceOf(UnauthorizedOperationException.class);
        notificationService.markAllRead(tech);
        assertThat(notificationService.unreadCount(tech)).isZero();
    }

    @Test
    void outbox_isForAdministratorsOnly() {
        report(payment, Impact.HIGH, Urgency.HIGH, 500);
        assertThat(notificationService.outbox(admin)).isNotEmpty();
        assertThatThrownBy(() -> notificationService.outbox(tech)).isInstanceOf(UnauthorizedOperationException.class);
    }
}
