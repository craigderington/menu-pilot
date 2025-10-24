package com.menupilot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class User {
    public enum Role { ADMIN, STAFF, MEMBER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) private Org org;
    @Column(nullable=false, unique=true) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Role role = Role.MEMBER;
    private boolean active = true;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
