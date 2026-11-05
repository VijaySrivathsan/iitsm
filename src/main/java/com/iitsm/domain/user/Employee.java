package com.iitsm.domain.user;

import com.iitsm.domain.Checks;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * An employee/customer who reports incidents and follows them until they are closed.
 */
@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    private String department;

    protected Employee() {
    }

    public Employee(String username, String passwordHash, String fullName, String email, String phone,
                    String department) {
        super(username, passwordHash, fullName, email, phone);
        this.department = Checks.optionalText(department);
    }

    @Override
    public Role getRole() {
        return Role.EMPLOYEE;
    }

    public void moveToDepartment(String department) {
        this.department = Checks.optionalText(department);
    }

    public String getDepartment() {
        return department;
    }
}
