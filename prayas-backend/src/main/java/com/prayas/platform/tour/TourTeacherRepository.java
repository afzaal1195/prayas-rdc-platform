package com.prayas.platform.tour;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TourTeacherRepository extends JpaRepository<TourTeacher, Long> {
    List<TourTeacher> findByProgrammeId(Long programmeId);
}