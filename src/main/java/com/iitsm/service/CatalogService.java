package com.iitsm.service;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.catalog.ITService;
import com.iitsm.domain.incident.Priority;
import com.iitsm.domain.sla.SlaPolicy;
import com.iitsm.exception.CategoryNotFoundException;
import com.iitsm.exception.InvalidInputException;
import com.iitsm.exception.ServiceNotFoundException;
import com.iitsm.repository.CategoryRepository;
import com.iitsm.repository.ITServiceRepository;
import com.iitsm.repository.SlaPolicyRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Administration of the service catalogue, incident categories and SLA policies. Changes are
 * restricted to administrators with {@code @PreAuthorize} (method security).
 */
@Service
@Transactional
public class CatalogService {

    private final ITServiceRepository services;
    private final CategoryRepository categories;
    private final SlaPolicyRepository slaPolicies;

    public CatalogService(ITServiceRepository services, CategoryRepository categories,
                          SlaPolicyRepository slaPolicies) {
        this.services = services;
        this.categories = categories;
        this.slaPolicies = slaPolicies;
    }

    // ---------------------------------------------------------------- services

    @Transactional(readOnly = true)
    public List<ITService> activeServices() {
        return services.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ITService> allServices() {
        return services.findAll(Sort.by("name"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ITService createService(String name, String description, int criticality) {
        if (services.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new InvalidInputException("A service called '" + name.trim() + "' already exists");
        }
        return services.save(new ITService(name, description, criticality));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ITService updateService(Long id, String description, int criticality, boolean active) {
        ITService service = services.findById(id).orElseThrow(() -> new ServiceNotFoundException(id));
        service.update(description, criticality);
        if (active) {
            service.activate();
        } else {
            service.deactivate();
        }
        return service;
    }

    // ---------------------------------------------------------------- categories

    @Transactional(readOnly = true)
    public List<Category> activeCategories() {
        return categories.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> allCategories() {
        return categories.findAll(Sort.by("name"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Category createCategory(String name, String description, String keywords) {
        if (categories.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new InvalidInputException("A category called '" + name.trim() + "' already exists");
        }
        return categories.save(new Category(name, description, keywords));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Category updateCategory(Long id, String description, String keywords, boolean active) {
        Category category = categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        category.update(description, keywords);
        if (active) {
            category.activate();
        } else {
            category.deactivate();
        }
        return category;
    }

    // ---------------------------------------------------------------- SLA policies

    /** Default policies first, then per service; within each, P1 to P4. */
    @Transactional(readOnly = true)
    public List<SlaPolicy> allSlaPolicies() {
        return slaPolicies.findAll().stream()
                .sorted(Comparator.comparing((SlaPolicy p) -> p.isDefault() ? "" : p.getService().getName())
                        .thenComparing(SlaPolicy::getPriority))
                .toList();
    }

    /**
     * Creates or updates the policy for (service, priority); a null service id means the default policy.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public SlaPolicy saveSlaPolicy(Long serviceId, Priority priority, int responseMinutes, int resolutionMinutes) {
        ITService service = serviceId == null ? null
                : services.findById(serviceId).orElseThrow(() -> new ServiceNotFoundException(serviceId));
        var existing = service == null
                ? slaPolicies.findByServiceIsNullAndPriority(priority)
                : slaPolicies.findByServiceAndPriority(service, priority);
        if (existing.isPresent()) {
            existing.get().changeTargets(responseMinutes, resolutionMinutes);
            return existing.get();
        }
        return slaPolicies.save(new SlaPolicy(service, priority, responseMinutes, resolutionMinutes));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSlaPolicy(Long policyId) {
        slaPolicies.findById(policyId).ifPresent(p -> {
            if (p.isDefault()) {
                throw new InvalidInputException("Default policies can be changed but not deleted");
            }
            slaPolicies.delete(p);
        });
    }
}
