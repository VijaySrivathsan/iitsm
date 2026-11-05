package com.iitsm.repository;

import com.iitsm.domain.user.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for employees (incident reporters).
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUsernameIgnoreCase(String username);
}
