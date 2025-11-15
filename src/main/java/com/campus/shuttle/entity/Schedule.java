package com.campus.shuttle.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(nullable = false)
    private String route;
    
    @Column(nullable = false)
    private LocalTime departureTime;
    
    @Column(nullable = false)
    private Integer totalSeats = 20;
    
    @Column(nullable = false)
    private Integer availableSeats = 20;

    public Schedule() {}

    public Schedule(LocalDate date, String route, LocalTime departureTime) {
        this.date = date;
        this.route = route;
        this.departureTime = departureTime;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    
    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
}