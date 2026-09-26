package com.nightjas50.invoicepilot.web;

import com.nightjas50.invoicepilot.domain.Client;
import com.nightjas50.invoicepilot.domain.Invoice;
import com.nightjas50.invoicepilot.domain.Money;
import com.nightjas50.invoicepilot.export.HtmlInvoiceExporter;
import com.nightjas50.invoicepilot.service.InvoiceService;
import com.nightjas50.invoicepilot.storage.InvoicePilotStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InvoicePilotWebServer {
    private final HttpServer server;
    private final InvoicePilotStore store;
    private final InvoiceService service;
    private final Path publicDir;
    private final ExecutorService executor;

    public InvoicePilotWebServer(int port, Path dataPath, Path publicDir) throws IOException {
        this.store = new InvoicePilotStore(dataPath);
        this.store.load();
        this.service = new InvoiceService(store);
        this.publicDir = publicDir;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.executor = Executors.newFixedThreadPool(6);
        this.server.setExecutor(executor);
        this.server.createContext("/", this::handle);
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path dataPath = args.length > 1 ? Path.of(args[1]) : Path.of("invoicepilot-data.tsv");
        Path publicDir = args.length > 2 ? Path.of(args[2]) : Path.of("src/main/resources/public");
        InvoicePilotWebServer app = new InvoicePilotWebServer(port, dataPath, publicDir);
        app.start();
        System.out.println("InvoicePilot web app running at http://localhost:" + app.port());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
            } else {
                serveStatic(exchange, path);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            sendJson(exchange, 400, "{" + JsonUtil.field("error", ex.getMessage()) + "}");
        } catch (Exception ex) {
            sendJson(exchange, 500, "{" + JsonUtil.field("error", "Unexpected server error") + "}");
        } finally {
            exchange.close();
        }
    }

    private void handleApi(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();
        if (method.equals("GET") && path.equals("/api/summary")) {
            summary(exchange);
        } else if (method.equals("GET") && path.equals("/api/clients")) {
            clients(exchange);
        } else if (method.equals("POST") && path.equals("/api/clients")) {
            addClient(exchange);
        } else if (method.equals("GET") && path.equals("/api/invoices")) {
            invoices(exchange);
        } else if (method.equals("POST") && path.equals("/api/invoices")) {
            createInvoice(exchange);
        } else if (method.equals("POST") && path.equals("/api/items")) {
            addItem(exchange);
        } else if (method.equals("POST") && path.equals("/api/invoices/sent")) {
            markSent(exchange);
        } else if (method.equals("POST") && path.equals("/api/invoices/paid")) {
            markPaid(exchange);
        } else if (method.equals("GET") && path.startsWith("/api/export/")) {
            exportInvoice(exchange, path.substring("/api/export/".length()));
        } else {
            sendJson(exchange, 404, "{" + JsonUtil.field("error", "Not found") + "}");
        }
    }

    private void summary(HttpExchange exchange) throws IOException {
        int paid = 0;
        int open = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : service.listInvoices()) {
            if (invoice.status().name().equals("PAID")) {
                paid++;
            } else {
                open++;
            }
            total = total.add(invoice.total().amount());
        }
        sendJson(exchange, 200, "{" +
                JsonUtil.numberField("clients", String.valueOf(service.listClients().size())) + "," +
                JsonUtil.numberField("invoices", String.valueOf(service.listInvoices().size())) + "," +
                JsonUtil.numberField("paid", String.valueOf(paid)) + "," +
                JsonUtil.numberField("open", String.valueOf(open)) + "," +
                JsonUtil.field("revenue", "USD " + total.setScale(2).toPlainString()) +
                "}");
    }

    private void clients(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, JsonUtil.array(service.listClients().stream().map(JsonUtil::clientJson).toList()));
    }

    private void addClient(HttpExchange exchange) throws IOException {
        Map<String, String> body = readBody(exchange);
        Client client = service.addClient(body.get("name"), body.get("email"), body.getOrDefault("company", ""), body.getOrDefault("address", ""));
        store.save();
        sendJson(exchange, 201, JsonUtil.clientJson(client));
    }

    private void invoices(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, JsonUtil.array(service.listInvoices().stream()
                .map(invoice -> JsonUtil.invoiceJson(invoice, service.getClient(invoice.clientId())))
                .toList()));
    }

    private void createInvoice(HttpExchange exchange) throws IOException {
        Map<String, String> body = readBody(exchange);
        Invoice invoice = service.createInvoice(
                body.get("clientId"),
                body.get("invoiceNumber"),
                LocalDate.parse(body.get("dueDate")),
                new BigDecimal(body.getOrDefault("taxRate", "0")),
                body.getOrDefault("currency", "USD"));
        store.save();
        sendJson(exchange, 201, JsonUtil.invoiceJson(invoice, service.getClient(invoice.clientId())));
    }

    private void addItem(HttpExchange exchange) throws IOException {
        Map<String, String> body = readBody(exchange);
        Invoice invoice = service.addItem(
                body.get("invoiceId"),
                body.get("description"),
                new BigDecimal(body.get("quantity")),
                Money.of(body.get("price"), body.getOrDefault("currency", "USD")));
        store.save();
        sendJson(exchange, 200, JsonUtil.invoiceJson(invoice, service.getClient(invoice.clientId())));
    }

    private void markSent(HttpExchange exchange) throws IOException {
        Map<String, String> body = readBody(exchange);
        Invoice invoice = service.markSent(body.get("invoiceId"));
        store.save();
        sendJson(exchange, 200, JsonUtil.invoiceJson(invoice, service.getClient(invoice.clientId())));
    }

    private void markPaid(HttpExchange exchange) throws IOException {
        Map<String, String> body = readBody(exchange);
        Invoice invoice = service.markPaid(body.get("invoiceId"));
        store.save();
        sendJson(exchange, 200, JsonUtil.invoiceJson(invoice, service.getClient(invoice.clientId())));
    }

    private void exportInvoice(HttpExchange exchange, String invoiceId) throws IOException {
        Invoice invoice = service.getInvoice(invoiceId);
        Client client = service.getClient(invoice.clientId());
        byte[] bytes = new HtmlInvoiceExporter().render(invoice, client).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private Map<String, String> readBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return new HashMap<>();
        }
        return JsonUtil.parseFlatJson(body);
    }

    private void serveStatic(HttpExchange exchange, String path) throws IOException {
        String normalized = path.equals("/") ? "/index.html" : path;
        Path file = publicDir.resolve(normalized.substring(1)).normalize();
        if (!file.startsWith(publicDir.normalize()) || !Files.exists(file) || Files.isDirectory(file)) {
            sendText(exchange, 404, "Not found", "text/plain; charset=utf-8");
            return;
        }
        sendText(exchange, 200, Files.readString(file, StandardCharsets.UTF_8), contentType(file));
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        return "text/plain; charset=utf-8";
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        sendText(exchange, status, body, "application/json; charset=utf-8");
    }

    private void sendText(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
