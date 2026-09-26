package com.nightjas50.invoicepilot;

import com.nightjas50.invoicepilot.cli.InvoicePilotApp;
import com.nightjas50.invoicepilot.domain.Client;
import com.nightjas50.invoicepilot.domain.Invoice;
import com.nightjas50.invoicepilot.domain.InvoiceStatus;
import com.nightjas50.invoicepilot.domain.LineItem;
import com.nightjas50.invoicepilot.domain.Money;
import com.nightjas50.invoicepilot.export.HtmlInvoiceExporter;
import com.nightjas50.invoicepilot.service.InvoiceService;
import com.nightjas50.invoicepilot.storage.InvoicePilotStore;
import com.nightjas50.invoicepilot.web.InvoicePilotWebServer;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public final class InvoicePilotTests {
    public static void main(String[] args) throws Exception {
        calculatesInvoiceTotals();
        persistsClientsAndInvoices();
        cliCreatesUsableDataFile();
        exportsHtmlInvoice();
        servesWebApi();
        System.out.println("All InvoicePilot tests passed");
    }

    private static void calculatesInvoiceTotals() {
        Invoice invoice = Invoice.create("INV-001", "client_1", LocalDate.now().plusDays(10), new BigDecimal("0.10"), "USD");
        invoice.addItem(new LineItem("Design work", new BigDecimal("2"), Money.of("100", "USD")));
        invoice.addItem(new LineItem("Hosting", new BigDecimal("1"), Money.of("50", "USD")));

        assertEquals("USD 250.00", invoice.subtotal().format());
        assertEquals("USD 25.00", invoice.tax().format());
        assertEquals("USD 275.00", invoice.total().format());
        invoice.markSent();
        assertEquals(InvoiceStatus.SENT, invoice.status());
    }

    private static void persistsClientsAndInvoices() throws Exception {
        Path data = Files.createTempFile("invoicepilot", ".tsv");
        InvoicePilotStore store = new InvoicePilotStore(data);
        InvoiceService service = new InvoiceService(store);
        Client client = service.addClient("Amina Lee", "amina@example.com", "Amina Studio", "Nairobi");
        Invoice invoice = service.createInvoice(client.id(), "INV-002", LocalDate.now().plusDays(7), BigDecimal.ZERO, "KES");
        service.addItem(invoice.id(), "Consulting", new BigDecimal("3"), Money.of("1500", "KES"));
        store.save();

        InvoicePilotStore loaded = new InvoicePilotStore(data);
        loaded.load();

        assertEquals(1, loaded.clients().size());
        assertEquals(1, loaded.invoices().size());
        assertEquals("KES 4500.00", loaded.invoices().getFirst().total().format());
    }

    private static void cliCreatesUsableDataFile() throws Exception {
        Path dir = Files.createTempDirectory("invoicepilot-cli");
        Path data = dir.resolve("data.tsv");
        InvoicePilotApp.run(new String[]{"add-client", "--data", data.toString(), "--name", "Client One", "--email", "one@example.com"});
        InvoicePilotStore store = new InvoicePilotStore(data);
        store.load();
        String clientId = store.clients().getFirst().id();

        InvoicePilotApp.run(new String[]{"create-invoice", "--data", data.toString(), "--client", clientId, "--number", "INV-003", "--due", "2030-01-01"});
        store.load();
        String invoiceId = store.invoices().getFirst().id();

        InvoicePilotApp.run(new String[]{"add-item", "--data", data.toString(), "--invoice", invoiceId, "--description", "Build app", "--quantity", "1", "--price", "500"});
        store.load();
        assertEquals("USD 500.00", store.invoices().getFirst().total().format());
    }

    private static void exportsHtmlInvoice() throws Exception {
        Client client = Client.create("Mika Stone", "mika@example.com", "", "123 Market Road");
        Invoice invoice = Invoice.create("INV-004", client.id(), LocalDate.now().plusDays(14), BigDecimal.ZERO, "USD");
        invoice.addItem(new LineItem("Prototype", BigDecimal.ONE, Money.of("750", "USD")));
        Path html = Files.createTempFile("invoice", ".html");

        new HtmlInvoiceExporter().export(invoice, client, html);

        String content = Files.readString(html);
        assertTrue(content.contains("Invoice INV-004"));
        assertTrue(content.contains("USD 750.00"));
    }

    private static void servesWebApi() throws Exception {
        Path data = Files.createTempFile("invoicepilot-web", ".tsv");
        Path publicDir = Files.createTempDirectory("invoicepilot-public");
        Files.writeString(publicDir.resolve("index.html"), "<h1>InvoicePilot</h1>");
        InvoicePilotWebServer server = new InvoicePilotWebServer(0, data, publicDir);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            URI base = URI.create("http://localhost:" + server.port());
            HttpRequest createClient = HttpRequest.newBuilder(base.resolve("/api/clients"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Web Client\",\"email\":\"web@example.com\"}"))
                    .build();
            HttpResponse<String> created = client.send(createClient, HttpResponse.BodyHandlers.ofString());
            assertEquals(201, created.statusCode());
            assertTrue(created.body().contains("Web Client"));

            HttpRequest summary = HttpRequest.newBuilder(base.resolve("/api/summary")).GET().build();
            HttpResponse<String> response = client.send(summary, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"clients\":1"));
        } finally {
            server.stop();
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }
}
