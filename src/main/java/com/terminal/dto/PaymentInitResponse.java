package com.terminal.dto;

public class PaymentInitResponse {
    private String wompiReference;
    private String invoiceNumber;
    private Long amountInCents;
    private String publicKey;
    private String currency;
    private String redirectUrl;
    private String integritySignature;

    public PaymentInitResponse() {}

    public String getWompiReference() { return wompiReference; }
    public void setWompiReference(String wompiReference) { this.wompiReference = wompiReference; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Long getAmountInCents() { return amountInCents; }
    public void setAmountInCents(Long amountInCents) { this.amountInCents = amountInCents; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    public String getIntegritySignature() { return integritySignature; }
    public void setIntegritySignature(String s) { this.integritySignature = s; }
}
