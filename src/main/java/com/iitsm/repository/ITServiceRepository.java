package com.iitsm.repository;

import com.iitsm.domain.catalog.ITService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the IT service catalogue.
 */
public interface ITServiceRepository extends JpaRepository<ITService, Long> {

    Optional<ITService> findByNameIgnoreCase(String name);

    List<ITService> findByActiveTrueOrderByNameAsc();
}
