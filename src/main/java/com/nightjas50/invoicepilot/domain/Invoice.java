package com.nightjas50.invoicepilot.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Invoice {
    private final String id;
    private final String invoiceNumber;
    private final String clientId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final BigDecimal taxRate;
    private final Money discount;
    private final String notes;
    private final List<LineItem> items;
    private InvoiceStatus status;

    public Invoice(
            String id,
            String invoiceNumber,
            String clientId,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal taxRate,
            Money discount,
            String notes,
            InvoiceStatus status,
            List<LineItem> items
    ) {
        this.id = requireText(id, "id");
        this.invoiceNumber = requireText(invoiceNumber, "invoice number");
        this.clientId = requireText(clientId, "client id");
        this.issueDate = issueDate == null ? LocalDate.now() : issueDate;
        this.dueDate = dueDate == null ? this.issueDate.plusDays(14) : dueDate;
        if (this.dueDate.isBefore(this.issueDate)) {
            throw new IllegalArgumentException("due date cannot be before issue date");
        }
        this.taxRate = taxRate == null ? BigDecimal.ZERO : taxRate;
        if (this.taxRate.signum() < 0) {
            throw new IllegalArgumentException("tax rate cannot be negative");
        }
        this.discount = discount == null ? Money.zero("USD") : discount;
        this.notes = notes == null ? "" : notes;
        this.status = status == null ? InvoiceStatus.DRAFT : status;
        this.items = new ArrayList<>();
        if (items != null) {
            items.forEach(this::addItem);
        }
    }

    public static Invoice create(String invoiceNumber, String clientId, LocalDate dueDate, BigDecimal taxRate, String currencyCode) {
        LocalDate issueDate = LocalDate.now();
        return new Invoice(
                "inv_" + UUID.randomUUID().toString().substring(0, 8),
                invoiceNumber,
                clientId,
                issueDate,
                dueDate,
                taxRate,
                Money.zero(currencyCode),
                "",
                InvoiceStatus.DRAFT,
                List.of()
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public void addItem(LineItem item) {
        if (!items.isEmpty() && !item.unitPrice().currencyCode().equals(currencyCode())) {
            throw new IllegalArgumentException("all line items must use the same currency");
        }
        items.add(item);
    }

    public Money subtotal() {
        Money total = Money.zero(currencyCode());
        for (LineItem item : items) {
            total = total.plus(item.total());
        }
        return total;
    }

    public Money taxableAmount() {
        Money subtotal = subtotal();
        if (discount.compareTo(subtotal) > 0) {
            return Money.zero(currencyCode());
        }
        return subtotal.minus(discount);
    }

    public Money tax() {
        return taxableAmount().multiply(taxRate);
    }

    public Money total() {
        return taxableAmount().plus(tax());
    }

    public void markSent() {
        if (items.isEmpty()) {
            throw new IllegalStateException("cannot send an invoice without line items");
        }
        status = InvoiceStatus.SENT;
    }

    public void markPaid() {
        status = InvoiceStatus.PAID;
    }

    public void refreshOverdueStatus(LocalDate today) {
        if (status == InvoiceStatus.SENT && today.isAfter(dueDate)) {
            status = InvoiceStatus.OVERDUE;
        }
    }

    public String currencyCode() {
        if (items.isEmpty()) {
            return discount.currencyCode();
        }
        return items.getFirst().unitPrice().currencyCode();
    }

    public String id() {
        return id;
    }

    public String invoiceNumber() {
        return invoiceNumber;
    }

    public String clientId() {
        return clientId;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public BigDecimal taxRate() {
        return taxRate;
    }

    public Money discount() {
        return discount;
    }

    public String notes() {
        return notes;
    }

    public InvoiceStatus status() {
        return status;
    }

    public List<LineItem> items() {
        return Collections.unmodifiableList(items);
    }
}
