package com.campus.shuttle.repository;

import com.campus.shuttle.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudentId(String studentId);
    List<Booking> findByScheduleId(Long scheduleId);
    boolean existsByScheduleIdAndSeatNumber(Long scheduleId, String seatNumber);
}