package com.menupilot.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
public class Preorder {
    public enum Status { CREATED, LOCKED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) private Event event;
    @ManyToOne(optional=false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Status status = Status.CREATED;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
