package com.iitsm.domain.user;

import com.iitsm.domain.Checks;
import com.iitsm.domain.catalog.Category;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A support engineer who investigates and resolves incidents.
 * Skills are an <b>aggregation</b> of {@link Category} objects: categories exist independently of
 * any technician and are shared between many technicians.
 */
@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    public static final int DEFAULT_MAX_WORKLOAD = 5;
    public static final int MAX_EXPERIENCE_LEVEL = 5;

    private Integer maxWorkload;
    private Boolean available;
    private Integer experienceLevel;
    private Boolean teamLead;

    @ManyToMany
    @JoinTable(name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> skills = new HashSet<>();

    protected Technician() {
    }

    public Technician(String username, String passwordHash, String fullName, String email, String phone,
                      int maxWorkload, int experienceLevel) {
        super(username, passwordHash, fullName, email, phone);
        this.maxWorkload = Checks.requireRange(maxWorkload, 1, 50, "Max workload");
        this.experienceLevel = Checks.requireRange(experienceLevel, 1, MAX_EXPERIENCE_LEVEL, "Experience level");
        this.available = true;
        this.teamLead = false;
    }

    @Override
    public Role getRole() {
        return Role.TECHNICIAN;
    }

    public void addSkill(Category category) {
        skills.add(Checks.requireNonNull(category, "Skill category"));
    }

    public void removeSkill(Category category) {
        skills.remove(category);
    }

    public boolean hasSkill(Category category) {
        return category != null && skills.contains(category);
    }

    /** Read-only view so callers cannot change skills without going through this class. */
    public Set<Category> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    /** True if taking one more incident would not exceed this technician's capacity. */
    public boolean hasCapacityFor(int currentActiveIncidents) {
        return currentActiveIncidents < getMaxWorkload();
    }

    public void changeMaxWorkload(int maxWorkload) {
        this.maxWorkload = Checks.requireRange(maxWorkload, 1, 50, "Max workload");
    }

    public void changeExperienceLevel(int experienceLevel) {
        this.experienceLevel = Checks.requireRange(experienceLevel, 1, MAX_EXPERIENCE_LEVEL, "Experience level");
    }

    public void markAvailable() {
        this.available = true;
    }

    public void markUnavailable() {
        this.available = false;
    }

    public void promoteToTeamLead() {
        this.teamLead = true;
    }

    public void removeTeamLead() {
        this.teamLead = false;
    }

    public int getMaxWorkload() {
        return maxWorkload == null ? DEFAULT_MAX_WORKLOAD : maxWorkload;
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }

    public int getExperienceLevel() {
        return experienceLevel == null ? 1 : experienceLevel;
    }

    public boolean isTeamLead() {
        return Boolean.TRUE.equals(teamLead);
    }
}
