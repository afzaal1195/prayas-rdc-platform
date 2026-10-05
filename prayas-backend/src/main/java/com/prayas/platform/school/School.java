package com.prayas.platform.school;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "school")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "village_or_town")
    private String villageOrTown;

    private String district;

    private String state;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected School() {
        // JPA
    }

    public School(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getVillageOrTown() {
        return villageOrTown;
    }

    public String getDistrict() {
        return district;
    }

    public String getState() {
        return state;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setVillageOrTown(String villageOrTown) {
        this.villageOrTown = villageOrTown;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public void setState(String state) {
        this.state = state;
    }
}