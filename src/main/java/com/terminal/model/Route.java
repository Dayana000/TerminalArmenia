package com.terminal.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origin;
    private String destination;
    private String schedule;
    private Double price;
    private Integer capacity;
    private Integer availableSeats;

    public Route() {}

    public Route(Long id, String origin, String destination, String schedule, Double price, Integer capacity, Integer availableSeats) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.schedule = schedule;
        this.price = price;
        this.capacity = capacity;
        this.availableSeats = availableSeats;
    }

    public Long getId() { return id; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getSchedule() { return schedule; }
    public Double getPrice() { return price; }
    public Integer getCapacity() { return capacity; }
    public Integer getAvailableSeats() { return availableSeats; }

    public void setId(Long id) { this.id = id; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public void setPrice(Double price) { this.price = price; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
}