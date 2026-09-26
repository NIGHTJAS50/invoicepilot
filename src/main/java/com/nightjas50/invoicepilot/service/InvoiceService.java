package com.nightjas50.invoicepilot.service;

import com.nightjas50.invoicepilot.domain.Client;
import com.nightjas50.invoicepilot.domain.Invoice;
import com.nightjas50.invoicepilot.domain.LineItem;
import com.nightjas50.invoicepilot.domain.Money;
import com.nightjas50.invoicepilot.storage.InvoicePilotStore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class InvoiceService {
    private final InvoicePilotStore store;

    public InvoiceService(InvoicePilotStore store) {
        this.store = store;
    }

    public Client addClient(String name, String email, String company, String address) {
        Client client = Client.create(name, email, company, address);
        store.putClient(client);
        return client;
    }

    public Invoice createInvoice(String clientId, String invoiceNumber, LocalDate dueDate, BigDecimal taxRate, String currencyCode) {
        store.findClient(clientId).orElseThrow(() -> new IllegalArgumentException("client not found: " + clientId));
        Invoice invoice = Invoice.create(invoiceNumber, clientId, dueDate, taxRate, currencyCode);
        store.putInvoice(invoice);
        return invoice;
    }

    public Invoice addItem(String invoiceId, String description, BigDecimal quantity, Money unitPrice) {
        Invoice invoice = getInvoice(invoiceId);
        invoice.addItem(new LineItem(description, quantity, unitPrice));
        return invoice;
    }

    public Invoice markSent(String invoiceId) {
        Invoice invoice = getInvoice(invoiceId);
        invoice.markSent();
        return invoice;
    }

    public Invoice markPaid(String invoiceId) {
        Invoice invoice = getInvoice(invoiceId);
        invoice.markPaid();
        return invoice;
    }

    public List<Client> listClients() {
        return store.clients().stream().sorted(Comparator.comparing(Client::name)).toList();
    }

    public List<Invoice> listInvoices() {
        return store.invoices().stream().sorted(Comparator.comparing(Invoice::invoiceNumber)).toList();
    }

    public Invoice getInvoice(String invoiceId) {
        return store.findInvoice(invoiceId).orElseThrow(() -> new IllegalArgumentException("invoice not found: " + invoiceId));
    }

    public Client getClient(String clientId) {
        return store.findClient(clientId).orElseThrow(() -> new IllegalArgumentException("client not found: " + clientId));
    }
}
