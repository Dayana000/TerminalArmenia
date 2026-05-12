package com.terminal.repository;

import com.terminal.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByWompiReference(String wompiReference);
    Optional<Payment> findByReservationId(Long reservationId);
    List<Payment> findByStatus(String status);
}
