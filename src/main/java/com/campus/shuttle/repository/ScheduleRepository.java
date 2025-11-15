package com.campus.shuttle.repository;

import com.campus.shuttle.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDateAndRoute(LocalDate date, String route);
    List<Schedule> findByDate(LocalDate date);
}