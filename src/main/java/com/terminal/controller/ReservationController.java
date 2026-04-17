package com.terminal.controller;

import com.terminal.model.Reservation;
import com.terminal.model.Route;
import com.terminal.model.User;
import com.terminal.repository.ReservationRepository;
import com.terminal.repository.RouteRepository;
import com.terminal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/reservations")
@CrossOrigin("*")
public class ReservationController {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private UserRepository userRepository;

    // POST /reservations — Crear reserva con validación completa
    @PostMapping
    public Object createReservation(@RequestBody Reservation reservation) {

        // Validar que lleguen los datos mínimos
        if (reservation.getUserId() == null)
            return ResponseEntity.badRequest().body("El ID del usuario es obligatorio");
        if (reservation.getRouteId() == null)
            return ResponseEntity.badRequest().body("El ID de la ruta es obligatorio");
        if (reservation.getSeat() == null || reservation.getSeat().isBlank())
            return ResponseEntity.badRequest().body("El asiento es obligatorio");

        // Buscar usuario
        Optional<User> userOpt = userRepository.findById(reservation.getUserId());
        if (userOpt.isEmpty())
            return ResponseEntity.badRequest().body("Usuario no encontrado");

        // Buscar ruta
        Optional<Route> routeOpt = routeRepository.findById(reservation.getRouteId());
        if (routeOpt.isEmpty())
            return ResponseEntity.badRequest().body("Ruta no encontrada");

        Route route = routeOpt.get();
        User user = userOpt.get();

        // Validar disponibilidad de cupos
        if (route.getAvailableSeats() == null || route.getAvailableSeats() <= 0)
            return ResponseEntity.badRequest().body("No hay asientos disponibles para esta ruta");

        // Descontar cupo automáticamente
        route.setAvailableSeats(route.getAvailableSeats() - 1);
        routeRepository.save(route);

        // Generar número único de reserva
        String reservationNumber = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Armar la reserva completa
        reservation.setReservationNumber(reservationNumber);
        reservation.setStatus("RESERVADA");
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setPassengerName(user.getName());
        reservation.setOrigin(route.getOrigin());
        reservation.setDestination(route.getDestination());
        reservation.setSchedule(route.getSchedule());
        reservation.setPrice(route.getPrice());

        Reservation saved = reservationRepository.save(reservation);
        return ResponseEntity.ok(saved);
    }

    // GET /reservations — Todas las reservas (ADMIN)
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    // GET /reservations/user/{userId} — Reservas de un usuario (PASAJERO)
    @GetMapping("/user/{userId}")
    public List<Reservation> getReservationsByUser(@PathVariable Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    // PUT /reservations/{id}/cancel — Cancelar reserva y devolver cupo
    @PutMapping("/{id}/cancel")
    public Object cancelReservation(@PathVariable Long id) {
        Optional<Reservation> resOpt = reservationRepository.findById(id);
        if (resOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Reservation reservation = resOpt.get();

        if ("CANCELADA".equals(reservation.getStatus()))
            return ResponseEntity.badRequest().body("La reserva ya está cancelada");

        // Devolver el cupo a la ruta
        Optional<Route> routeOpt = routeRepository.findById(reservation.getRouteId());
        routeOpt.ifPresent(route -> {
            route.setAvailableSeats(route.getAvailableSeats() + 1);
            routeRepository.save(route);
        });

        reservation.setStatus("CANCELADA");
        reservationRepository.save(reservation);
        return ResponseEntity.ok(reservation);
    }

    // PUT /reservations/{id}/confirm — Confirmar reserva (ADMIN)
    @PutMapping("/{id}/confirm")
    public Object confirmReservation(@PathVariable Long id) {
        Optional<Reservation> resOpt = reservationRepository.findById(id);
        if (resOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Reservation reservation = resOpt.get();

        if ("CANCELADA".equals(reservation.getStatus()))
            return ResponseEntity.badRequest().body("No se puede confirmar una reserva cancelada");

        reservation.setStatus("CONFIRMADA");
        reservationRepository.save(reservation);
        return ResponseEntity.ok(reservation);
    }
}
