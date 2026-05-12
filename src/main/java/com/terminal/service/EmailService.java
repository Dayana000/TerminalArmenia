package com.terminal.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.*;
import com.terminal.model.Payment;
import com.terminal.model.Reservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    // ── CODIGO DE VERIFICACION ───────────────────────────────────
    public void sendVerificationEmail(String toEmail, String name, String code) throws IOException {
        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail, "Terminal de Transportes Armenia"));
        mail.setSubject("Codigo de verificacion - Terminal Armenia");

        Personalization personalization = new Personalization();
        personalization.addTo(new Email(toEmail));
        mail.addPersonalization(personalization);

        Content content = new Content("text/html", buildVerificationHtml(name, code));
        mail.addContent(content);

        send(mail);
    }

    // ── CONFIRMACION DE PAGO CON PDFs ────────────────────────────
    public void sendConfirmationEmail(Reservation reservation, Payment payment,
                                      String ticketPdfPath, String invoicePdfPath) throws IOException {
        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail, "Terminal de Transportes Armenia"));
        mail.setSubject("Reserva Confirmada - " + reservation.getReservationNumber());

        Personalization personalization = new Personalization();
        personalization.addTo(new Email(payment.getCustomerEmail()));
        mail.addPersonalization(personalization);

        Content content = new Content("text/html", buildConfirmationHtml(reservation, payment));
        mail.addContent(content);

        // Adjuntar tiquete PDF
        if (ticketPdfPath != null) {
            File f = new File(ticketPdfPath);
            if (f.exists()) {
                Attachments attachment = new Attachments();
                attachment.setContent(Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));
                attachment.setType("application/pdf");
                attachment.setFilename("Tiquete-" + reservation.getReservationNumber() + ".pdf");
                attachment.setDisposition("attachment");
                mail.addAttachments(attachment);
            }
        }

        // Adjuntar factura PDF
        if (invoicePdfPath != null) {
            File f = new File(invoicePdfPath);
            if (f.exists()) {
                Attachments attachment = new Attachments();
                attachment.setContent(Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));
                attachment.setType("application/pdf");
                attachment.setFilename("Factura-" + payment.getInvoiceNumber() + ".pdf");
                attachment.setDisposition("attachment");
                mail.addAttachments(attachment);
            }
        }

        send(mail);
    }

    private void send(Mail mail) throws IOException {
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);
        if (response.getStatusCode() >= 400) {
            throw new IOException("SendGrid error: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    // ── HTML: CODIGO DE VERIFICACION ────────────────────────────
    private String buildVerificationHtml(String name, String code) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
                + "<style>"
                + "body{margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',sans-serif}"
                + ".wrap{max-width:520px;margin:32px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08)}"
                + ".hdr{background:linear-gradient(135deg,#1e3a8a,#2563eb);padding:32px 40px;text-align:center;color:#fff}"
                + ".hdr h1{margin:0;font-size:22px;font-weight:700}"
                + ".body{padding:32px 40px;text-align:center}"
                + ".code-box{background:#eff6ff;border:2px dashed #3b82f6;border-radius:14px;padding:28px;margin:24px 0}"
                + ".code{font-size:42px;font-weight:800;color:#1e3a8a;letter-spacing:10px;font-family:monospace}"
                + ".note{font-size:13px;color:#6b7280;margin-top:20px}"
                + ".foot{background:#f8fafc;padding:20px 40px;text-align:center;font-size:12px;color:#9ca3af;border-top:1px solid #e5e7eb}"
                + "</style></head><body>"
                + "<div class='wrap'>"
                + "<div class='hdr'><div style='font-size:40px;margin-bottom:8px'>&#9993;</div>"
                + "<h1>Verifica tu correo</h1>"
                + "<p style='margin:6px 0 0;opacity:.85;font-size:14px'>Terminal de Transportes de Armenia</p></div>"
                + "<div class='body'>"
                + "<p style='font-size:15px;color:#374151'>Hola <strong>" + name + "</strong>, ingresa este codigo para verificar tu cuenta:</p>"
                + "<div class='code-box'>"
                + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:.08em;color:#6b7280;margin-bottom:8px'>Tu codigo de verificacion</div>"
                + "<div class='code'>" + code + "</div>"
                + "</div>"
                + "<p class='note'>Este codigo es valido por 15 minutos.<br>Si no solicitaste este registro, ignora este correo.</p>"
                + "</div>"
                + "<div class='foot'>Terminal de Transportes de Armenia | Carrera 15 Calle 12 Norte | Tel: (57)6 735 9300</div>"
                + "</div></body></html>";
    }

    // ── HTML: CONFIRMACION DE PAGO ───────────────────────────────
    private String buildConfirmationHtml(Reservation r, Payment p) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fecha = r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "-";

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
                + "<style>"
                + "body{margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',sans-serif}"
                + ".wrap{max-width:600px;margin:32px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08)}"
                + ".hdr{background:linear-gradient(135deg,#1e3a8a,#2563eb);padding:36px 40px;text-align:center;color:#fff}"
                + ".hdr h1{margin:0;font-size:24px;font-weight:700}"
                + ".body{padding:36px 40px}"
                + ".res-num{background:#eff6ff;border:2px dashed #3b82f6;border-radius:12px;text-align:center;padding:20px;margin-bottom:28px}"
                + ".route{background:linear-gradient(135deg,#eff6ff,#dbeafe);border-radius:12px;padding:20px;text-align:center;margin-bottom:24px}"
                + ".grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:24px}"
                + ".cell{background:#f8fafc;border-radius:10px;padding:14px 16px}"
                + ".cell .k{font-size:11px;color:#9ca3af;margin-bottom:2px}"
                + ".cell .v{font-size:14px;font-weight:600;color:#111827}"
                + ".total{background:#1e3a8a;color:#fff;border-radius:12px;padding:16px 20px;display:flex;justify-content:space-between;align-items:center;margin-bottom:24px}"
                + ".note{background:#f0fdf4;border-left:4px solid #22c55e;border-radius:6px;padding:14px 16px;font-size:13px;color:#166534;margin-bottom:24px}"
                + ".foot{background:#f8fafc;padding:24px 40px;text-align:center;font-size:12px;color:#9ca3af;border-top:1px solid #e5e7eb}"
                + "</style></head><body>"
                + "<div class='wrap'>"
                + "<div class='hdr'><div style='font-size:48px;margin-bottom:8px'>&#10003;</div>"
                + "<h1>Tu reserva esta confirmada</h1>"
                + "<p style='margin:6px 0 0;opacity:.85;font-size:14px'>Pago procesado - Terminal de Transportes de Armenia</p></div>"
                + "<div class='body'>"
                + "<div class='res-num'>"
                + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:.1em;color:#6b7280'>N de Reserva</div>"
                + "<div style='font-size:28px;font-weight:800;color:#1e3a8a;font-family:monospace'>" + r.getReservationNumber() + "</div>"
                + "</div>"
                + "<div class='route'>"
                + "<div style='font-size:20px;font-weight:700;color:#1e3a8a'>" + r.getOrigin() + " &rarr; " + r.getDestination() + "</div>"
                + "<div style='font-size:13px;color:#374151;margin-top:6px'>Horario: " + r.getSchedule() + "</div>"
                + "</div>"
                + "<div class='grid'>"
                + "<div class='cell'><div class='k'>Pasajero</div><div class='v'>" + r.getPassengerName() + "</div></div>"
                + "<div class='cell'><div class='k'>Asiento</div><div class='v'>" + r.getSeat() + "</div></div>"
                + "<div class='cell'><div class='k'>Factura</div><div class='v'>" + p.getInvoiceNumber() + "</div></div>"
                + "<div class='cell'><div class='k'>Fecha</div><div class='v'>" + fecha + "</div></div>"
                + "<div class='cell'><div class='k'>Metodo de Pago</div><div class='v'>" + (p.getPaymentMethod() != null ? p.getPaymentMethod() : "Simulado") + "</div></div>"
                + "<div class='cell'><div class='k'>Estado</div><div class='v' style='color:#16a34a'>CONFIRMADA</div></div>"
                + "</div>"
                + "<div class='total'><span style='font-size:13px;opacity:.8'>Total Pagado</span>"
                + "<span style='font-size:22px;font-weight:800'>$ " + String.format("%,.0f", r.getPrice()) + " COP</span></div>"
                + "<div class='note'>Adjuntos encontraras tu <strong>tiquete digital PDF</strong> con codigo QR "
                + "y tu <strong>factura electronica</strong>. Presentalo al momento del abordaje.</div>"
                + "</div>"
                + "<div class='foot'>Terminal de Transportes de Armenia | Carrera 15 Calle 12 Norte | Tel: (57)6 735 9300</div>"
                + "</div></body></html>";
    }
}