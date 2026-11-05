package com.iitsm.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-registration form (employees only). Validated with Bean Validation before reaching the service.
 */
public class RegistrationForm {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "3-50 letters, digits, dot, underscore or dash")
    private String username;

    @NotBlank
    @Size(min = 6, max = 100, message = "at least 6 characters")
    private String password;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Email
    @Size(max = 120)
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9 ]{7,20}$", message = "digits only, optional leading +")
    private String phone;

    @Size(max = 100)
    private String department;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
