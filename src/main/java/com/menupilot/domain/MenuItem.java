package com.menupilot.domain;

import jakarta.persistence.*;

@Entity
public class MenuItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) private Event event;
    @Column(nullable=false) private String name;
    private String description;
    private Integer priceCents;
    private Integer capQty;
    private String station;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public Integer getCapQty() { return capQty; }
    public void setCapQty(Integer capQty) { this.capQty = capQty; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
}
