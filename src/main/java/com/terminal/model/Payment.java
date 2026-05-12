package com.terminal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;        // FAC-XXXXXXXX

    @Column(nullable = false)
    private Long reservationId;

    private String wompiTransactionId;   // ID que devuelve Wompi al aprobar
    private String wompiReference;       // Referencia que enviamos a Wompi (única)

    private Double amount;               // Valor en pesos COP
    private String currency;             // COP

    // PENDIENTE_PAGO | APROBADO | RECHAZADO | CANCELADO | EXPIRADO
    private String status;

    private String paymentMethod;        // CARD, PSE, NEQUI, BANCOLOMBIA_TRANSFER
    private String customerEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Payment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public String getWompiTransactionId() { return wompiTransactionId; }
    public void setWompiTransactionId(String wompiTransactionId) { this.wompiTransactionId = wompiTransactionId; }
    public String getWompiReference() { return wompiReference; }
    public void setWompiReference(String wompiReference) { this.wompiReference = wompiReference; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}