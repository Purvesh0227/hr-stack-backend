package com.hrstack.hr_stack.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;


@Entity
@Table(name = "employee",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "mobile")
        }
)
public class Employee {

    //UUID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //FirstName
    @NotBlank(message = "First name is required")
    @Pattern(
            regexp = "^\\S+$",
            message = "First name must not contain spaces"
    )
    @Column(name = "first_name", nullable = false)
    private String firstName;

    //Lastname
    @NotBlank(message = "Last name is required")
    @Pattern(
            regexp = "^\\S+$",
            message = "Last name must not contain spaces"
    )
    @Column(name = "last_name", nullable = false)
    private String lastName;

    //email
    @NotBlank(message = "Email is required")
    @Pattern(
            regexp ="^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "invalid email format"
    )
    @Column(unique = true)
    private String email;

    //phone
    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Mobile number must be 10 digits"
    )
    @Column( name = "mobile",nullable = false)
    private String mobile;

    //password
    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!.*_-]).{8,}$",
            message = "Password must be at least 8 characters and include an" +
                    " uppercase letter, a lowercase letter, a digit, and a special character"

    )
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;


    //Role
    private String role;

    public Employee() {
    }

    public Employee(UUID id, String firstName, String lastName, String email, String mobile,String password,String role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.role = role;

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
