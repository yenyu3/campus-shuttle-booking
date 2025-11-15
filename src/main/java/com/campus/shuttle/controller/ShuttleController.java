package com.campus.shuttle.controller;

import com.campus.shuttle.entity.Booking;
import com.campus.shuttle.entity.Schedule;
import com.campus.shuttle.service.ShuttleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ShuttleController {

    @Autowired
    private ShuttleService shuttleService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "studentId", username));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "登入失敗"));
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<Schedule>> searchSchedules(
            @RequestParam String date,
            @RequestParam(required = false) String route) {
        LocalDate searchDate = LocalDate.parse(date);
        List<Schedule> schedules = shuttleService.searchSchedules(searchDate, route);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/bookings/{studentId}")
    public ResponseEntity<List<Booking>> getBookings(@PathVariable String studentId) {
        List<Booking> bookings = shuttleService.getBookingsByStudent(studentId);
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> bookingData) {
        try {
            String studentId = (String) bookingData.get("studentId");
            Long scheduleId = Long.valueOf(bookingData.get("scheduleId").toString());
            String seatNumber = (String) bookingData.get("seatNumber");

            Booking booking = shuttleService.createBooking(studentId, scheduleId, seatNumber);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam String studentId) {
        try {
            shuttleService.cancelBooking(bookingId, studentId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/init")
    public ResponseEntity<?> initializeData() {
        shuttleService.initializeSchedules();
        return ResponseEntity.ok(Map.of("message", "資料初始化完成"));
    }
}