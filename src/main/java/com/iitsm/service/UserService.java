package com.iitsm.service;

import com.iitsm.domain.catalog.Category;
import com.iitsm.domain.user.Administrator;
import com.iitsm.domain.user.Employee;
import com.iitsm.domain.user.NewUser;
import com.iitsm.domain.user.Role;
import com.iitsm.domain.user.Technician;
import com.iitsm.domain.user.User;
import com.iitsm.domain.user.UserFactory;
import com.iitsm.exception.InvalidInputException;
import com.iitsm.exception.UnauthorizedOperationException;
import com.iitsm.exception.UserNotFoundException;
import com.iitsm.repository.CategoryRepository;
import com.iitsm.repository.TechnicianRepository;
import com.iitsm.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * User management: self-registration of employees, and administrator management of users and
 * technicians (skills, capacity, availability, team-lead flag).
 */
@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final TechnicianRepository technicians;
    private final CategoryRepository categories;
    private final UserFactory userFactory;

    public UserService(UserRepository users, TechnicianRepository technicians, CategoryRepository categories,
                       UserFactory userFactory) {
        this.users = users;
        this.technicians = technicians;
        this.categories = categories;
        this.userFactory = userFactory;
    }

    /** Anyone may register, but only as an employee. */
    public Employee registerEmployee(NewUser details, String department) {
        requireFreeUsername(details.username());
        return users.save(userFactory.createEmployee(details, department));
    }

    /** An administrator creates a user of any role. */
    @PreAuthorize("hasRole('ADMIN')")
    public User createUser(Role role, NewUser details) {
        requireFreeUsername(details.username());
        return users.save(userFactory.create(role, details));
    }

    @Transactional(readOnly = true)
    public List<User> allUsers() {
        return users.findAll(Sort.by("username"));
    }

    @Transactional(readOnly = true)
    public List<Technician> allTechnicians() {
        return technicians.findAll(Sort.by("username"));
    }

    @Transactional(readOnly = true)
    public User find(Long id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Technician findTechnician(Long id) {
        return technicians.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    /** Activates or deactivates an account. Administrators cannot deactivate themselves. */
    @PreAuthorize("hasRole('ADMIN')")
    public User setActive(Long userId, boolean active, User actor) {
        User user = find(userId);
        if (!active && user.equals(actor)) {
            throw new InvalidInputException("You cannot deactivate your own account");
        }
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        return user;
    }

    /** Administrator updates a technician's skills and capacity. */
    @PreAuthorize("hasRole('ADMIN')")
    public Technician updateTechnician(Long technicianId, Set<Long> skillIds, int maxWorkload, int experienceLevel,
                                       boolean teamLead, boolean available) {
        Technician tech = findTechnician(technicianId);
        List<Category> wanted = skillIds == null ? List.of() : categories.findAllById(skillIds);
        List.copyOf(tech.getSkills()).forEach(tech::removeSkill);
        wanted.forEach(tech::addSkill);
        tech.changeMaxWorkload(maxWorkload);
        tech.changeExperienceLevel(experienceLevel);
        if (teamLead) {
            tech.promoteToTeamLead();
        } else {
            tech.removeTeamLead();
        }
        setAvailability(tech, available);
        return tech;
    }

    /** A technician marks themselves available or away (e.g. on leave); away = no new assignments. */
    public Technician setOwnAvailability(User actor, boolean available) {
        if (!(actor instanceof Technician)) {
            throw new UnauthorizedOperationException("Only technicians have an availability status");
        }
        Technician tech = findTechnician(actor.getId());
        setAvailability(tech, available);
        return tech;
    }

    private static void setAvailability(Technician tech, boolean available) {
        if (available) {
            tech.markAvailable();
        } else {
            tech.markUnavailable();
        }
    }

    private void requireFreeUsername(String username) {
        if (username != null && users.existsByUsernameIgnoreCase(username.trim())) {
            throw new InvalidInputException("Username '" + username.trim() + "' is already taken");
        }
    }

    /** True if the user is an administrator (helper for views). */
    public static boolean isAdmin(User user) {
        return user instanceof Administrator;
    }
}
