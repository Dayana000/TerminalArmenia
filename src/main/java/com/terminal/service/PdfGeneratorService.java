package com.terminal.service;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.terminal.model.Payment;
import com.terminal.model.Reservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorService {

    @Value("${app.pdf.output-dir:./pdfs}")
    private String outputDir;

    private static final DeviceRgb DARK_BLUE  = new DeviceRgb(30,  58,  138);
    private static final DeviceRgb MID_BLUE   = new DeviceRgb(37,  99,  235);
    private static final DeviceRgb LIGHT_BLUE = new DeviceRgb(239, 246, 255);
    private static final DeviceRgb GRAY       = new DeviceRgb(107, 114, 128);

    // ── TIQUETE ─────────────────────────────────────────────────
    public String generateTicketPdf(Reservation r, Payment p) throws IOException {
        Files.createDirectories(Paths.get(outputDir));
        String path = outputDir + "/tiquete-" + r.getReservationNumber() + ".pdf";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        PdfDocument pdf = new PdfDocument(new PdfWriter(path));
        Document doc = new Document(pdf, PageSize.A5);
        doc.setMargins(28, 28, 28, 28);

        // Encabezado + QR
        Table top = new Table(UnitValue.createPercentArray(new float[]{65, 35})).useAllAvailableWidth();
        top.addCell(new Cell()
                .add(new Paragraph("TERMINAL DE TRANSPORTES").setFontSize(12).setBold().setFontColor(DARK_BLUE))
                .add(new Paragraph("Armenia · Quindío").setFontSize(9).setFontColor(GRAY))
                .add(new Paragraph("TIQUETE DIGITAL").setFontSize(10).setBold().setFontColor(MID_BLUE))
                .setBorder(Border.NO_BORDER).setPaddingBottom(6));

        String qrContent = "RES:" + r.getReservationNumber()
                + "|PAS:" + r.getPassengerName()
                + "|RUT:" + r.getOrigin() + "-" + r.getDestination();
        BarcodeQRCode qr = new BarcodeQRCode(qrContent);
        PdfFormXObject xobj = qr.createFormXObject(ColorConstants.BLACK, pdf);
        top.addCell(new Cell().add(new Image(xobj).setWidth(75).setHeight(75))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
        doc.add(top);

        doc.add(new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .setFontColor(MID_BLUE).setFontSize(7).setMarginTop(4).setMarginBottom(4));

        // Número de reserva
        doc.add(new Paragraph(r.getReservationNumber())
                .setFontSize(24).setBold().setFontColor(DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        doc.add(new Paragraph("Número de Reserva").setFontSize(8).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(12));

        // Ruta
        Table route = new Table(UnitValue.createPercentArray(new float[]{42, 16, 42}))
                .useAllAvailableWidth().setBackgroundColor(LIGHT_BLUE).setMarginBottom(12);
        route.addCell(new Cell()
                .add(new Paragraph(r.getOrigin()).setFontSize(13).setBold().setFontColor(DARK_BLUE))
                .add(new Paragraph("ORIGEN").setFontSize(8).setFontColor(GRAY))
                .setBorder(Border.NO_BORDER).setPadding(10).setTextAlignment(TextAlignment.CENTER));
        route.addCell(new Cell()
                .add(new Paragraph("→").setFontSize(18).setBold().setFontColor(MID_BLUE))
                .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER));
        route.addCell(new Cell()
                .add(new Paragraph(r.getDestination()).setFontSize(13).setBold().setFontColor(DARK_BLUE))
                .add(new Paragraph("DESTINO").setFontSize(8).setFontColor(GRAY))
                .setBorder(Border.NO_BORDER).setPadding(10).setTextAlignment(TextAlignment.CENTER));
        doc.add(route);

        // Detalles
        Table det = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        det.addCell(infoCell("Pasajero",       r.getPassengerName()));
        det.addCell(infoCell("Asiento",        r.getSeat()));
        det.addCell(infoCell("Horario",        r.getSchedule()));
        det.addCell(infoCell("Fecha emisión",  r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "—"));
        det.addCell(infoCell("Método de pago", p.getPaymentMethod() != null ? p.getPaymentMethod() : "Wompi"));
        det.addCell(infoCell("Factura",        p.getInvoiceNumber()));
        doc.add(det);

        // Total
        doc.add(new Paragraph("$ " + String.format("%,.0f", r.getPrice()) + " COP")
                .setFontSize(16).setBold().setFontColor(DARK_BLUE)
                .setTextAlignment(TextAlignment.RIGHT).setMarginTop(10));
        doc.add(new Paragraph("Valor pagado").setFontSize(8).setFontColor(GRAY)
                .setTextAlignment(TextAlignment.RIGHT));

        // Pie
        doc.add(new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .setFontColor(MID_BLUE).setFontSize(7).setMarginTop(8).setMarginBottom(4));
        doc.add(new Paragraph("Válido únicamente para la ruta y horario indicados · " +
                "Carrera 15 Calle 12 Norte, Armenia · Tel: (57)6 735 9300")
                .setFontSize(7).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return new File(path).getAbsolutePath();
    }

    // ── FACTURA ─────────────────────────────────────────────────
    public String generateInvoicePdf(Reservation r, Payment p) throws IOException {
        Files.createDirectories(Paths.get(outputDir));
        String path = outputDir + "/factura-" + p.getInvoiceNumber() + ".pdf";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        PdfDocument pdf = new PdfDocument(new PdfWriter(path));
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);

        // Encabezado
        Table hdr = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth();
        hdr.addCell(new Cell()
                .add(new Paragraph("TERMINAL DE TRANSPORTES DE ARMENIA").setFontSize(13).setBold().setFontColor(DARK_BLUE))
                .add(new Paragraph("NIT: 890.000.000-1").setFontSize(9).setFontColor(GRAY))
                .add(new Paragraph("Carrera 15 Calle 12 Norte, Armenia, Quindío").setFontSize(9).setFontColor(GRAY))
                .add(new Paragraph("Tel: (57)6 735 9300").setFontSize(9).setFontColor(GRAY))
                .setBorder(Border.NO_BORDER));
        hdr.addCell(new Cell()
                .add(new Paragraph("FACTURA ELECTRÓNICA").setFontSize(10).setBold()
                        .setFontColor(ColorConstants.WHITE).setBackgroundColor(DARK_BLUE)
                        .setTextAlignment(TextAlignment.CENTER).setPadding(4))
                .add(new Paragraph(p.getInvoiceNumber()).setFontSize(16).setBold()
                        .setFontColor(DARK_BLUE).setTextAlignment(TextAlignment.CENTER).setMarginTop(4))
                .add(new Paragraph("Fecha: " + (p.getCreatedAt() != null ? p.getCreatedAt().format(fmt) : "—"))
                        .setFontSize(9).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER))
                .setBorder(new SolidBorder(DARK_BLUE, 1)).setPadding(8));
        doc.add(hdr);
        doc.add(new Paragraph(" "));

        // Datos del cliente
        sectionTitle(doc, "DATOS DEL CLIENTE");
        Table client = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
        addRow(client, "Nombre",     r.getPassengerName());
        addRow(client, "Correo",     p.getCustomerEmail());
        addRow(client, "N° Reserva", r.getReservationNumber());
        doc.add(client);
        doc.add(new Paragraph(" "));

        // Detalle del servicio
        sectionTitle(doc, "DETALLE DEL SERVICIO");
        Table svc = new Table(UnitValue.createPercentArray(new float[]{45, 15, 20, 20})).useAllAvailableWidth();
        for (String h : new String[]{"Descripción", "Cant.", "Valor Unit.", "Total"}) {
            svc.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontSize(10).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(MID_BLUE).setBorder(Border.NO_BORDER).setPadding(6));
        }
        String desc  = "Tiquete " + r.getOrigin() + " → " + r.getDestination()
                + "\nAsiento: " + r.getSeat() + " | Horario: " + r.getSchedule();
        String valor = "$ " + String.format("%,.0f", r.getPrice());
        svc.addCell(new Cell().add(new Paragraph(desc).setFontSize(10)).setPadding(6));
        svc.addCell(new Cell().add(new Paragraph("1").setFontSize(10)).setTextAlignment(TextAlignment.CENTER).setPadding(6));
        svc.addCell(new Cell().add(new Paragraph(valor).setFontSize(10)).setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        svc.addCell(new Cell().add(new Paragraph(valor).setFontSize(10)).setTextAlignment(TextAlignment.RIGHT).setPadding(6));
        doc.add(svc);
        doc.add(new Paragraph(" "));

        // Totales
        Table totals = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
        addTotalRow(totals, "Subtotal",  valor, false);
        addTotalRow(totals, "IVA (0%)", "$ 0",  false);
        addTotalRow(totals, "TOTAL COP", valor, true);
        doc.add(totals);
        doc.add(new Paragraph(" "));

        // Información de pago
        sectionTitle(doc, "INFORMACIÓN DE PAGO");
        Table pay = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
        addRow(pay, "Método",         p.getPaymentMethod() != null ? p.getPaymentMethod() : "Wompi");
        addRow(pay, "ID Transacción", p.getWompiTransactionId() != null ? p.getWompiTransactionId() : "—");
        addRow(pay, "Estado",         "APROBADO");
        doc.add(pay);
        doc.add(new Paragraph(" "));

        // Pie
        doc.add(new Paragraph("Documento generado automáticamente por el sistema de la Terminal de " +
                "Transportes de Armenia. Consérvelo como comprobante de su compra.")
                .setFontSize(8).setFontColor(GRAY).setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(new SolidBorder(GRAY, 0.5f)).setPaddingTop(10));

        doc.close();
        return new File(path).getAbsolutePath();
    }

    // ── HELPERS ─────────────────────────────────────────────────
    private Cell infoCell(String label, String value) {
        return new Cell()
                .add(new Paragraph(label).setFontSize(8).setFontColor(GRAY).setMarginBottom(1))
                .add(new Paragraph(value != null ? value : "—").setFontSize(11).setBold())
                .setBackgroundColor(LIGHT_BLUE).setBorder(Border.NO_BORDER).setPadding(8);
    }

    private void sectionTitle(Document doc, String title) {
        doc.add(new Paragraph(title).setFontSize(10).setBold().setFontColor(DARK_BLUE)
                .setBackgroundColor(LIGHT_BLUE).setPadding(6).setMarginBottom(0));
    }

    private void addRow(Table t, String label, String value) {
        t.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(10))
                .setBackgroundColor(LIGHT_BLUE).setPadding(6).setBorder(Border.NO_BORDER));
        t.addCell(new Cell().add(new Paragraph(value != null ? value : "—").setFontSize(10))
                .setPadding(6).setBorder(Border.NO_BORDER));
    }

    private void addTotalRow(Table t, String label, String value, boolean highlight) {
        Paragraph lp = new Paragraph(label).setFontSize(11);
        Paragraph vp = new Paragraph(value).setFontSize(11);
        if (highlight) { lp.setBold().setFontColor(DARK_BLUE); vp.setBold().setFontColor(DARK_BLUE); }
        t.addCell(new Cell().add(lp).setTextAlignment(TextAlignment.RIGHT)
                .setPadding(6).setBorder(Border.NO_BORDER));
        t.addCell(new Cell().add(vp).setTextAlignment(TextAlignment.RIGHT)
                .setPadding(6).setBorder(highlight ? new SolidBorder(DARK_BLUE, 1.5f) : Border.NO_BORDER));
    }
}