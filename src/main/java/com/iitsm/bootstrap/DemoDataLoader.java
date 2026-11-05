package com.iitsm.bootstrap;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.user.NewUser;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.UserFactory;
import com.iitsm.repository.CategoryRepository;
import com.iitsm.repository.ITServiceRepository;
import com.iitsm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Seeds an EMPTY database with the demo accounts (docs/demo-accounts.md), IT services and incident
 * categories so the app can be used straight away. Does nothing if any user already exists, so it is
 * safe on a persistent PostgreSQL database. Disable with {@code itsm.demo-data.enabled=false}.
 *
 * <p>Local demo data only: every password is "demo123".
 */
@Component
@Order(10) // after DefaultSlaPolicyInitializer
@ConditionalOnProperty(name = "itsm.demo-data.enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    static final String DEMO_PASSWORD = "demo123";

    private final UserRepository users;
    private final ITServiceRepository services;
    private final CategoryRepository categories;
    private final UserFactory userFactory;

    public DemoDataLoader(UserRepository users, ITServiceRepository services, CategoryRepository categories,
                          UserFactory userFactory) {
        this.users = users;
        this.services = services;
        this.categories = categories;
        this.userFactory = userFactory;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        Map<String, Category> cat = seedCategories();
        seedServices();
        seedUsers(cat);
        log.info("Demo data created: {} users, {} services, {} categories (password '{}')",
                users.count(), services.count(), categories.count(), DEMO_PASSWORD);
    }

    private Map<String, Category> seedCategories() {
        String[][] rows = {
                {"Hardware", "Laptops, printers, devices", "laptop, printer, monitor, keyboard, mouse, device, hardware"},
                {"Software", "Installed applications", "install, software, license, update, crash, excel, office"},
                {"Network", "Connectivity, VPN, Wi-Fi", "network, vpn, wifi, internet, connection, latency, dns"},
                {"Database", "Database servers and queries", "database, sql, query, db, table, deadlock, timeout"},
                {"Cloud", "Cloud platforms and hosting", "cloud, aws, azure, server, instance, container"},
                {"Security", "Security incidents", "security, virus, malware, phishing, breach, attack"},
                {"Authentication", "Login, SSO, passwords", "login, password, sso, account, locked, authentication, otp"},
                {"Application", "Business applications", "application, app, payment, transaction, checkout, portal, error"},
                {"Storage", "File shares and storage", "storage, disk, drive, file, share, backup, quota"}};
        Map<String, Category> result = new HashMap<>();
        for (String[] r : rows) {
            Category c = categories.save(new Category(r[0], r[1], r[2]));
            result.put(c.getName(), c);
        }
        return result;
    }

    private void seedServices() {
        services.save(new ITService("Payment Gateway", "Card and UPI payments for customers", 5));
        services.save(new ITService("Auth / SSO", "Single sign-on for all internal apps", 5));
        services.save(new ITService("Core Database", "Main PostgreSQL cluster", 4));
        services.save(new ITService("VPN", "Remote access for employees", 4));
        services.save(new ITService("Email", "Corporate email and calendar", 3));
        services.save(new ITService("Cloud Storage", "Shared drives and backups", 3));
        services.save(new ITService("HR Portal", "Leave, payroll and HR requests", 2));
    }

    private void seedUsers(Map<String, Category> cat) {
        users.save(userFactory.createEmployee(details("emp1", "Asha Rao", "+919000000001"), "Finance"));
        users.save(userFactory.createEmployee(details("emp2", "Rahul Mehta", "+919000000002"), "Sales"));
        users.save(userFactory.createEmployee(details("emp3", "Priya Nair", "+919000000003"), "Operations"));

        technician("tech1", "Karan Shah", "+919000000011", 5, 5, cat, "APPLICATION", "DATABASE");
        technician("tech2", "Divya Iyer", "+919000000012", 5, 3, cat, "NETWORK", "CLOUD");
        technician("tech3", "Arjun Das", "+919000000013", 5, 3, cat, "AUTHENTICATION", "SECURITY");
        Technician lead = technician("lead1", "Meera Pillai", "+919000000014", 4, 4, cat,
                "APPLICATION", "NETWORK", "HARDWARE", "SOFTWARE", "STORAGE");
        lead.promoteToTeamLead();

        users.save(userFactory.createAdministrator(details("sysadmin", "Vikram Rao", "+919000000021"), false));
        users.save(userFactory.createAdministrator(details("admin", "Anita Menon", "+919000000022"), true));
    }

    private Technician technician(String username, String name, String phone, int maxWorkload, int experience,
                                  Map<String, Category> cat, String... skills) {
        Technician tech = userFactory.createTechnician(details(username, name, phone), maxWorkload, experience);
        for (String skill : skills) {
            tech.addSkill(cat.get(skill));
        }
        return users.save(tech);
    }

    private static NewUser details(String username, String fullName, String phone) {
        return new NewUser(username, DEMO_PASSWORD, fullName, username + "@iitsm.local", phone);
    }
}
