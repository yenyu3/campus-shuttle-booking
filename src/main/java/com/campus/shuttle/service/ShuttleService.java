package com.campus.shuttle.service;

import com.campus.shuttle.entity.Booking;
import com.campus.shuttle.entity.Schedule;
import com.campus.shuttle.repository.BookingRepository;
import com.campus.shuttle.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShuttleService {
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    public List<Schedule> searchSchedules(LocalDate date, String route) {
        if (route != null && !route.isEmpty()) {
            return scheduleRepository.findByDateAndRoute(date, route);
        }
        return scheduleRepository.findByDate(date);
    }

    public List<Booking> getBookingsByStudent(String studentId) {
        return bookingRepository.findByStudentId(studentId);
    }

    @Transactional
    public Booking createBooking(String studentId, Long scheduleId, String seatNumber) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new RuntimeException("班次不存在");
        }

        Schedule schedule = scheduleOpt.get();
        
        if (bookingRepository.existsByScheduleIdAndSeatNumber(scheduleId, seatNumber)) {
            throw new RuntimeException("座位已被預約");
        }

        if (schedule.getAvailableSeats() <= 0) {
            throw new RuntimeException("班次已滿");
        }

        Booking booking = new Booking(studentId, schedule, seatNumber);
        booking = bookingRepository.save(booking);

        schedule.setAvailableSeats(schedule.getAvailableSeats() - 1);
        scheduleRepository.save(schedule);

        return booking;
    }

    @Transactional
    public void cancelBooking(Long bookingId, String studentId) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new RuntimeException("預約不存在");
        }

        Booking booking = bookingOpt.get();
        if (!booking.getStudentId().equals(studentId)) {
            throw new RuntimeException("無權限取消此預約");
        }

        Schedule schedule = booking.getSchedule();
        schedule.setAvailableSeats(schedule.getAvailableSeats() + 1);
        scheduleRepository.save(schedule);

        bookingRepository.delete(booking);
    }

    public void initializeSchedules() {
        if (scheduleRepository.count() == 0) {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            
            String[] routes = {"校門↔宿舍", "圖書館↔工程三館", "行政大樓↔體育館"};
            LocalTime[] times = {
                LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
                LocalTime.of(13, 30), LocalTime.of(15, 0), LocalTime.of(16, 30)
            };

            for (LocalDate date : new LocalDate[]{today, tomorrow}) {
                for (String route : routes) {
                    for (LocalTime time : times) {
                        scheduleRepository.save(new Schedule(date, route, time));
                    }
                }
            }
        }
    }
}