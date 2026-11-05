package com.iitsm.domain.user;

import com.iitsm.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserFactoryTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost keeps tests fast
    private final UserFactory factory = new UserFactory(encoder);

    private static NewUser details(String username, String password) {
        return new NewUser(username, password, "Test Person", "test@example.com", "9999999999");
    }

    @Test
    void create_returnsCorrectSubtypeForEachRole() {
        assertThat(factory.create(Role.EMPLOYEE, details("emp9", "secret1"))).isInstanceOf(Employee.class);
        assertThat(factory.create(Role.TECHNICIAN, details("tech9", "secret1"))).isInstanceOf(Technician.class);
        assertThat(factory.create(Role.ADMIN, details("admin9", "secret1"))).isInstanceOf(Administrator.class);
    }

    @Test
    void create_roleIsReportedPolymorphically() {
        User user = factory.create(Role.TECHNICIAN, details("tech9", "secret1"));
        assertThat(user.getRole()).isEqualTo(Role.TECHNICIAN);
        assertThat(user.getRole().authority()).isEqualTo("ROLE_TECHNICIAN");
    }

    @Test
    void create_storesBcryptHashNotRawPassword() {
        User user = factory.createEmployee(details("emp9", "secret1"), "Finance");
        assertThat(user.getPasswordHash()).isNotEqualTo("secret1").startsWith("$2");
        assertThat(encoder.matches("secret1", user.getPasswordHash())).isTrue();
    }

    @Test
    void create_normalisesUsernameToLowerCase() {
        assertThat(factory.createEmployee(details("Emp9", "secret1"), null).getUsername()).isEqualTo("emp9");
    }

    @Test
    void createTechnician_hasDefaultsAvailableAndNotTeamLead() {
        Technician tech = factory.createTechnician(details("tech9", "secret1"), 4, 3);
        assertThat(tech.isAvailable()).isTrue();
        assertThat(tech.isTeamLead()).isFalse();
        assertThat(tech.getMaxWorkload()).isEqualTo(4);
        assertThat(tech.getSkills()).isEmpty();
    }

    @Test
    void create_rejectsPasswordShorterThanMinimum() {
        assertThatThrownBy(() -> factory.createEmployee(details("emp9", "12345"), null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("at least 6");
    }

    @Test
    void create_rejectsBlankOrInvalidUsername() {
        assertThatThrownBy(() -> factory.createEmployee(details("  ", "secret1"), null))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> factory.createEmployee(details("bad name!", "secret1"), null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void createTechnician_rejectsOutOfRangeExperience() {
        assertThatThrownBy(() -> factory.createTechnician(details("tech9", "secret1"), 5, 6))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Experience level");
    }

    @Test
    void newUser_toStringNeverRevealsPassword() {
        assertThat(details("emp9", "secret1").toString()).doesNotContain("secret1");
    }
}
