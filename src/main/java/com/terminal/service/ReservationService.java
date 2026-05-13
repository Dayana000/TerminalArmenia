package com.terminal.service;

import com.terminal.model.Reservation;
import com.terminal.model.Route;
import com.terminal.model.User;
import com.terminal.repository.ReservationRepository;
import com.terminal.repository.RouteRepository;
import com.terminal.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private UserRepository userRepository;

    // ── CREAR RESERVA ─────────────────────────────────────────────
    public Reservation createReservation(Reservation reservation) {
        if (reservation.getUserId() == null)
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        if (reservation.getRouteId() == null)
            throw new IllegalArgumentException("El ID de la ruta es obligatorio");
        if (reservation.getSeat() == null || reservation.getSeat().isBlank())
            throw new IllegalArgumentException("El asiento es obligatorio");

        User user = userRepository.findById(reservation.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Route route = routeRepository.findById(reservation.getRouteId())
                .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada"));

        if (route.getAvailableSeats() == null || route.getAvailableSeats() <= 0)
            throw new IllegalArgumentException("No hay asientos disponibles para esta ruta");

        boolean seatTaken = reservationRepository.findByRouteId(reservation.getRouteId())
                .stream()
                .anyMatch(r -> r.getSeat().equals(reservation.getSeat())
                        && !"CANCELADA".equals(r.getStatus()));

        if (seatTaken)
            throw new IllegalArgumentException(
                    "El asiento " + reservation.getSeat() + " ya esta reservado en esta ruta");

        // Descontar cupo
        route.setAvailableSeats(route.getAvailableSeats() - 1);
        routeRepository.save(route);

        String reservationNumber = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        reservation.setReservationNumber(reservationNumber);
        reservation.setStatus("RESERVADA");
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setPassengerName(user.getName());
        reservation.setOrigin(route.getOrigin());
        reservation.setDestination(route.getDestination());
        reservation.setSchedule(route.getSchedule());
        reservation.setPrice(route.getPrice());

        Reservation saved = reservationRepository.save(reservation);
        log.info("[ReservationService] Reserva creada: {} para usuario {}", reservationNumber, user.getEmail());
        return saved;
    }

    // ── LISTAR TODAS ─────────────────────────────────────────────
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    // ── RESERVAS POR USUARIO ──────────────────────────────────────
    public List<Reservation> getReservationsByUser(Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    // ── CANCELAR RESERVA ──────────────────────────────────────────
    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada"));

        if ("CANCELADA".equals(reservation.getStatus()))
            throw new IllegalArgumentException("La reserva ya esta cancelada");

        // Devolver cupo
        Optional<Route> routeOpt = routeRepository.findById(reservation.getRouteId());
        routeOpt.ifPresent(route -> {
            route.setAvailableSeats(route.getAvailableSeats() + 1);
            routeRepository.save(route);
        });

        reservation.setStatus("CANCELADA");
        Reservation saved = reservationRepository.save(reservation);
        log.info("[ReservationService] Reserva cancelada: {}", reservation.getReservationNumber());
        return saved;
    }

    // ── CONFIRMAR RESERVA (ADMIN) ─────────────────────────────────
    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada"));

        if ("CANCELADA".equals(reservation.getStatus()))
            throw new IllegalArgumentException("No se puede confirmar una reserva cancelada");

        reservation.setStatus("CONFIRMADA");
        return reservationRepository.save(reservation);
    }

    // ── OBTENER ASIENTOS OCUPADOS ─────────────────────────────────
    public List<String> getTakenSeatsForRoute(Long routeId) {
        return reservationRepository.findByRouteId(routeId).stream()
                .filter(r -> !"CANCELADA".equals(r.getStatus()))
                .map(Reservation::getSeat)
                .toList();
    }
}
