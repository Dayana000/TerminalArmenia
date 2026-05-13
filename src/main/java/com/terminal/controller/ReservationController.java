package com.terminal.controller;

import com.terminal.model.Reservation;
import com.terminal.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired private ReservationService reservationService;

    // POST /reservations — Crear reserva (requiere autenticacion)
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Reservation reservation) {
        try {
            Reservation saved = reservationService.createReservation(reservation);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /reservations — Todas las reservas (solo ADMIN)
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    // GET /reservations/user/{userId} — Reservas de un usuario
    @GetMapping("/user/{userId}")
    public List<Reservation> getReservationsByUser(@PathVariable Long userId) {
        return reservationService.getReservationsByUser(userId);
    }

    // PUT /reservations/{id}/cancel — Cancelar reserva y devolver cupo
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            Reservation cancelled = reservationService.cancelReservation(id);
            return ResponseEntity.ok(cancelled);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /reservations/{id}/confirm — Confirmar reserva (solo ADMIN)
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmReservation(@PathVariable Long id) {
        try {
            Reservation confirmed = reservationService.confirmReservation(id);
            return ResponseEntity.ok(confirmed);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /reservations/route/{routeId}/seats — Obtener asientos ocupados de una ruta
    @GetMapping("/route/{routeId}/seats")
    public ResponseEntity<List<String>> getTakenSeatsForRoute(@PathVariable Long routeId) {
        List<String> takenSeats = reservationService.getTakenSeatsForRoute(routeId);
        return ResponseEntity.ok(takenSeats);
    }
}