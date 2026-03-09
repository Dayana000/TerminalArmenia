package com.terminal.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long routeId;
    private String seat;

    public Reservation() {
    }

    public Reservation(Long id, Long userId, Long routeId, String seat) {
        this.id = id;
        this.userId = userId;
        this.routeId = routeId;
        this.seat = seat;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public String getSeat() {
        return seat;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public void setSeat(String seat) {
        this.seat = seat;
    }
}