package com.menupilot.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="event")
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) private Org org;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private OffsetDateTime startsAt;
    private OffsetDateTime cutoffAt;
    @Column(length=2000) private String notes;
    private String coverImagePath; // file path for PDF cover
    private String themeAccentHex; // e.g., "#333333" or "333333"

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getCutoffAt() { return cutoffAt; }
    public void setCutoffAt(OffsetDateTime cutoffAt) { this.cutoffAt = cutoffAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }
    public String getThemeAccentHex() { return themeAccentHex; }
    public void setThemeAccentHex(String themeAccentHex) { this.themeAccentHex = themeAccentHex; }
}
