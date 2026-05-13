package com.terminal.controller;

import com.terminal.dto.PaymentInitRequest;
import com.terminal.dto.PaymentInitResponse;
import com.terminal.dto.SimulatePaymentRequest;
import com.terminal.model.Payment;
import com.terminal.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * POST /payments/init
     * Crea el registro de pago y devuelve la referencia unica.
     */
    @PostMapping("/init")
    public ResponseEntity<?> initPayment(@RequestBody PaymentInitRequest req) {
        try {
            PaymentInitResponse response = paymentService.initPayment(req);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /payments/simulate
     * Simula el resultado del pago (APROBADO o RECHAZADO).
     * Reemplaza el webhook de Wompi para la demo universitaria.
     */
    @PostMapping("/simulate")
    public ResponseEntity<?> simulatePayment(@RequestBody SimulatePaymentRequest req) {
        try {
            Payment payment = paymentService.processPaymentSimulation(
                    req.getReference(), req.getStatus());
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /payments/status/{reference}
     * El frontend hace polling aqui para saber el estado del pago.
     */
    @GetMapping("/status/{reference}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String reference) {
        try {
            Payment payment = paymentService.verifyPaymentByReference(reference);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}