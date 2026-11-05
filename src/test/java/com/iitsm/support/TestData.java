package com.iitsm.support;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.Technician;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * A small organisation for service-layer tests, mirroring the demo data: three employees, a skilled
 * technician, a network technician, a team lead, a system administrator and an IT manager.
 */
public class TestData {

    public final Employee emp1;
    public final Employee emp2;
    public final Employee emp3;
    public final Technician tech1;
    public final Technician tech2;
    public final Technician lead1;
    public final Administrator sysadmin;
    public final Administrator admin;
    public final Category application;
    public final Category network;
    public final ITService payment;
    public final ITService vpn;
    public final ITService hrPortal;

    public TestData(TestEntityManager em) {
        emp1 = em.persist(new Employee("emp1", "h", "Asha Rao", "asha@corp.test", null, "Finance"));
        emp2 = em.persist(new Employee("emp2", "h", "Rahul Mehta", "rahul@corp.test", null, "Sales"));
        emp3 = em.persist(new Employee("emp3", "h", "Priya Nair", "priya@corp.test", null, "Ops"));
        application = em.persist(new Category("Application", null, "payment, app"));
        network = em.persist(new Category("Network", null, "vpn, wifi"));
        tech1 = new Technician("tech1", "h", "Karan Shah", "karan@corp.test", null, 5, 5);
        tech1.addSkill(application);
        em.persist(tech1);
        tech2 = new Technician("tech2", "h", "Divya Iyer", "divya@corp.test", null, 5, 3);
        tech2.addSkill(network);
        em.persist(tech2);
        lead1 = new Technician("lead1", "h", "Meera Pillai", "meera@corp.test", null, 4, 4);
        lead1.promoteToTeamLead();
        em.persist(lead1);
        sysadmin = em.persist(new Administrator("sysadmin", "h", "Vikram Rao", "vikram@corp.test", null, false));
        admin = em.persist(new Administrator("admin", "h", "Anita Menon", "anita@corp.test", null, true));
        payment = em.persist(new ITService("Payment Gateway", null, 5));
        vpn = em.persist(new ITService("VPN", null, 4));
        hrPortal = em.persist(new ITService("HR Portal", null, 2));
    }
}
