package com.prayas.platform.tour;

import jakarta.persistence.*;

/** An accompanying teacher/faculty coordinator on a campus tour request. */
@Entity
@Table(name = "tour_teacher")
public class TourTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    protected TourTeacher() {
        // JPA
    }

    public TourTeacher(Programme programme, String fullName, String phone) {
        this.programme = programme;
        this.fullName = fullName;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }
}