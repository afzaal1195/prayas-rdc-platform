package com.prayas.platform.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "domain")
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. HOSPITALITY_LOGISTICS

    @Column(nullable = false, unique = true)
    private String name; // e.g. Hospitality & Logistics

    protected Domain() {
        // JPA
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
