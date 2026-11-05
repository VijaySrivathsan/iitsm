package com.iitsm.repository;

import com.iitsm.domain.user.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for administrators (also used to find escalation targets).
 */
public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    List<Administrator> findByActiveTrue();

    List<Administrator> findByItManagerTrueAndActiveTrue();
}
