package com.nightjas50.invoicepilot.storage;

import com.nightjas50.invoicepilot.domain.Client;
import com.nightjas50.invoicepilot.domain.Invoice;
import com.nightjas50.invoicepilot.domain.InvoiceStatus;
import com.nightjas50.invoicepilot.domain.LineItem;
import com.nightjas50.invoicepilot.domain.Money;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InvoicePilotStore {
    private final Path path;
    private final Map<String, Client> clients = new LinkedHashMap<>();
    private final Map<String, Invoice> invoices = new LinkedHashMap<>();

    public InvoicePilotStore(Path path) {
        this.path = path;
    }

    public void load() throws IOException {
        clients.clear();
        invoices.clear();
        if (!Files.exists(path)) {
            return;
        }
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t", -1);
            switch (columns[0]) {
                case "CLIENT" -> loadClient(columns);
                case "INVOICE" -> loadInvoice(columns);
                case "ITEM" -> loadItem(columns);
                default -> throw new IOException("Unknown record type: " + columns[0]);
            }
        }
    }

    public void save() throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        List<String> lines = new ArrayList<>();
        lines.add("# InvoicePilot data v1");
        for (Client client : clients.values()) {
            lines.add(String.join("\t",
                    "CLIENT",
                    enc(client.id()),
                    enc(client.name()),
                    enc(client.email()),
                    enc(client.company()),
                    enc(client.address())));
        }
        for (Invoice invoice : invoices.values()) {
            lines.add(String.join("\t",
                    "INVOICE",
                    enc(invoice.id()),
                    enc(invoice.invoiceNumber()),
                    enc(invoice.clientId()),
                    invoice.issueDate().toString(),
                    invoice.dueDate().toString(),
                    invoice.taxRate().toPlainString(),
                    invoice.discount().amount().toPlainString(),
                    invoice.discount().currencyCode(),
                    enc(invoice.notes()),
                    invoice.status().name()));
            for (LineItem item : invoice.items()) {
                lines.add(String.join("\t",
                        "ITEM",
                        enc(invoice.id()),
                        enc(item.description()),
                        item.quantity().toPlainString(),
                        item.unitPrice().amount().toPlainString(),
                        item.unitPrice().currencyCode()));
            }
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    public void putClient(Client client) {
        clients.put(client.id(), client);
    }

    public void putInvoice(Invoice invoice) {
        invoices.put(invoice.id(), invoice);
    }

    public Optional<Client> findClient(String clientId) {
        return Optional.ofNullable(clients.get(clientId));
    }

    public Optional<Invoice> findInvoice(String invoiceId) {
        return Optional.ofNullable(invoices.get(invoiceId));
    }

    public List<Client> clients() {
        return List.copyOf(clients.values());
    }

    public List<Invoice> invoices() {
        return List.copyOf(invoices.values());
    }

    private void loadClient(String[] columns) throws IOException {
        requireColumns(columns, 6);
        Client client = new Client(dec(columns[1]), dec(columns[2]), dec(columns[3]), dec(columns[4]), dec(columns[5]));
        clients.put(client.id(), client);
    }

    private void loadInvoice(String[] columns) throws IOException {
        requireColumns(columns, 11);
        Invoice invoice = new Invoice(
                dec(columns[1]),
                dec(columns[2]),
                dec(columns[3]),
                LocalDate.parse(columns[4]),
                LocalDate.parse(columns[5]),
                new BigDecimal(columns[6]),
                Money.of(columns[7], columns[8]),
                dec(columns[9]),
                InvoiceStatus.valueOf(columns[10]),
                List.of());
        invoices.put(invoice.id(), invoice);
    }

    private void loadItem(String[] columns) throws IOException {
        requireColumns(columns, 6);
        Invoice invoice = invoices.get(dec(columns[1]));
        if (invoice == null) {
            throw new IOException("Line item references unknown invoice");
        }
        invoice.addItem(new LineItem(dec(columns[2]), new BigDecimal(columns[3]), Money.of(columns[4], columns[5])));
    }

    private void requireColumns(String[] columns, int expected) throws IOException {
        if (columns.length != expected) {
            throw new IOException("Invalid data file record");
        }
    }

    private static String enc(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
