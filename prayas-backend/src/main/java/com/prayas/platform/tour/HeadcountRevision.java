package com.prayas.platform.tour;

import com.prayas.platform.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "headcount_revision")
public class HeadcountRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @Column(name = "revision_no", nullable = false)
    private int revisionNo;

    @Column(name = "student_count", nullable = false)
    private int studentCount;

    @Column(name = "teacher_count", nullable = false)
    private int teacherCount;

    @Column(name = "veg_meals")
    private Integer vegMeals;

    @Column(name = "non_veg_meals")
    private Integer nonVegMeals;

    @Column(columnDefinition = "text")
    private String reason;

    // Null means the school itself made the change (public endpoint).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private AppUser changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected HeadcountRevision() {
        // JPA
    }

    public HeadcountRevision(Programme programme, int revisionNo, int studentCount,
                              int teacherCount, AppUser changedBy) {
        this.programme = programme;
        this.revisionNo = revisionNo;
        this.studentCount = studentCount;
        this.teacherCount = teacherCount;
        this.changedBy = changedBy;
    }

    public int getRevisionNo() {
        return revisionNo;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public int getTeacherCount() {
        return teacherCount;
    }

    public Integer getVegMeals() {
        return vegMeals;
    }

    public void setVegMeals(Integer vegMeals) {
        this.vegMeals = vegMeals;
    }

    public void setNonVegMeals(Integer nonVegMeals) {
        this.nonVegMeals = nonVegMeals;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}