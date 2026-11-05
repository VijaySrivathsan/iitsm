package com.iitsm.repository;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.user.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for technicians, including the skill lookups used by assignment strategies.
 */
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByUsernameIgnoreCase(String username);

    /** Active technicians who have the given skill. */
    List<Technician> findBySkillsContainingAndActiveTrue(Category skill);

    List<Technician> findByActiveTrue();

    List<Technician> findByTeamLeadTrueAndActiveTrue();
}
