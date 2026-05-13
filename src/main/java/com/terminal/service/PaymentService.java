package com.terminal.service;

import com.terminal.dto.PaymentInitRequest;
import com.terminal.dto.PaymentInitResponse;
import com.terminal.model.Payment;
import com.terminal.model.Reservation;
import com.terminal.model.Route;
import com.terminal.repository.PaymentRepository;
import com.terminal.repository.ReservationRepository;
import com.terminal.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RouteRepository routeRepository;
    @Autowired private PdfGeneratorService pdfGeneratorService;
    @Autowired private EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    // ─────────────────────────────────────────────────────────────
    // 1. INICIAR PAGO
    //    Crea el Payment con PENDIENTE_PAGO y devuelve la referencia
    //    que el frontend usa para identificar la transaccion.
    // ─────────────────────────────────────────────────────────────
    public PaymentInitResponse initPayment(PaymentInitRequest req) {

        Reservation reservation = reservationRepository
                .findById(req.getReservationId())
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!"RESERVADA".equals(reservation.getStatus()) &&
                !"PENDIENTE_PAGO".equals(reservation.getStatus())) {
            throw new RuntimeException("La reserva no esta en estado valido para pago");
        }

        Route route = routeRepository
                .findById(reservation.getRouteId())
                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));

        if (route.getAvailableSeats() == null || route.getAvailableSeats() < 0) {
            throw new RuntimeException("No hay cupos disponibles para esta ruta");
        }

        String wompiRef   = "TRM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String invoiceNum = "FAC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        long amountCents  = Math.round(reservation.getPrice() * 100);

        Payment payment = new Payment();
        payment.setReservationId(reservation.getId());
        payment.setWompiReference(wompiRef);
        payment.setInvoiceNumber(invoiceNum);
        payment.setAmount(reservation.getPrice());
        payment.setCurrency("COP");
        payment.setStatus("PENDIENTE_PAGO");
        payment.setCustomerEmail(req.getCustomerEmail());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        reservation.setStatus("PENDIENTE_PAGO");
        reservationRepository.save(reservation);

        PaymentInitResponse response = new PaymentInitResponse();
        response.setWompiReference(wompiRef);
        response.setInvoiceNumber(invoiceNum);
        response.setAmountInCents(amountCents);
        response.setPublicKey("SIMULADO");
        response.setCurrency("COP");
        response.setRedirectUrl(baseUrl + "/payment-result.html");
        response.setIntegritySignature("SIMULADO");
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // 2. SIMULAR PAGO (reemplaza el webhook de Wompi)
    //    El frontend llama este metodo con APROBADO o RECHAZADO.
    // ─────────────────────────────────────────────────────────────
    public Payment processPaymentSimulation(String reference, String status) {

        Payment payment = paymentRepository.findByWompiReference(reference)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado: " + reference));

        payment.setPaymentMethod("SIMULADO");
        payment.setUpdatedAt(LocalDateTime.now());

        if ("APROBADO".equals(status)) {
            payment.setStatus("APROBADO");
            payment.setWompiTransactionId("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            paymentRepository.save(payment);
            confirmarReservaYNotificar(payment);
        } else {
            payment.setStatus("RECHAZADO");
            paymentRepository.save(payment);
            liberarCupo(payment.getReservationId());
        }

        return payment;
    }

    // ─────────────────────────────────────────────────────────────
    // 3. VERIFICAR ESTADO (polling desde frontend)
    // ─────────────────────────────────────────────────────────────
    public Payment verifyPaymentByReference(String wompiReference) {
        return paymentRepository.findByWompiReference(wompiReference)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────
    private void confirmarReservaYNotificar(Payment payment) {
        reservationRepository.findById(payment.getReservationId()).ifPresent(reservation -> {
            reservation.setStatus("CONFIRMADA");
            reservationRepository.save(reservation);
            try {
                String ticketPath  = pdfGeneratorService.generateTicketPdf(reservation, payment);
                String invoicePath = pdfGeneratorService.generateInvoicePdf(reservation, payment);
                if (payment.getCustomerEmail() != null && !payment.getCustomerEmail().isBlank()) {
                    emailService.sendConfirmationEmail(reservation, payment, ticketPath, invoicePath);
                }
            } catch (Exception e) {
                log.error("[PaymentService] Error generando PDFs o enviando correo para reserva {}: {}",
                        payment.getReservationId(), e.getMessage(), e);
            }
        });
    }

    private void liberarCupo(Long reservationId) {
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            reservation.setStatus("CANCELADA");
            reservationRepository.save(reservation);
            routeRepository.findById(reservation.getRouteId()).ifPresent(route -> {
                route.setAvailableSeats(route.getAvailableSeats() + 1);
                routeRepository.save(route);
            });
        });
    }
}