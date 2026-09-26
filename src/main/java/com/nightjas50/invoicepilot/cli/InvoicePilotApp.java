package com.nightjas50.invoicepilot.cli;

import com.nightjas50.invoicepilot.domain.Client;
import com.nightjas50.invoicepilot.domain.Invoice;
import com.nightjas50.invoicepilot.domain.Money;
import com.nightjas50.invoicepilot.export.HtmlInvoiceExporter;
import com.nightjas50.invoicepilot.service.InvoiceService;
import com.nightjas50.invoicepilot.storage.InvoicePilotStore;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class InvoicePilotApp {
    private InvoicePilotApp() {
    }

    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }

    public static int run(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("help")) {
            printHelp();
            return 0;
        }

        Map<String, String> options = parseOptions(Arrays.copyOfRange(args, 1, args.length));
        Path dataPath = Path.of(options.getOrDefault("--data", "invoicepilot-data.tsv"));
        InvoicePilotStore store = new InvoicePilotStore(dataPath);
        store.load();
        InvoiceService service = new InvoiceService(store);

        switch (args[0]) {
            case "add-client" -> {
                Client client = service.addClient(
                        required(options, "--name"),
                        required(options, "--email"),
                        options.getOrDefault("--company", ""),
                        options.getOrDefault("--address", ""));
                store.save();
                System.out.println("Created client " + client.id() + " (" + client.name() + ")");
                return 0;
            }
            case "list-clients" -> {
                for (Client client : service.listClients()) {
                    System.out.printf("%s  %s  <%s>%n", client.id(), client.name(), client.email());
                }
                return 0;
            }
            case "create-invoice" -> {
                Invoice invoice = service.createInvoice(
                        required(options, "--client"),
                        required(options, "--number"),
                        LocalDate.parse(required(options, "--due")),
                        new BigDecimal(options.getOrDefault("--tax", "0")),
                        options.getOrDefault("--currency", "USD"));
                store.save();
                System.out.println("Created invoice " + invoice.id() + " (" + invoice.invoiceNumber() + ")");
                return 0;
            }
            case "add-item" -> {
                Invoice invoice = service.addItem(
                        required(options, "--invoice"),
                        required(options, "--description"),
                        new BigDecimal(required(options, "--quantity")),
                        Money.of(required(options, "--price"), options.getOrDefault("--currency", "USD")));
                store.save();
                System.out.println("Invoice total is now " + invoice.total().format());
                return 0;
            }
            case "mark-sent" -> {
                Invoice invoice = service.markSent(required(options, "--invoice"));
                store.save();
                System.out.println("Invoice " + invoice.invoiceNumber() + " marked SENT");
                return 0;
            }
            case "mark-paid" -> {
                Invoice invoice = service.markPaid(required(options, "--invoice"));
                store.save();
                System.out.println("Invoice " + invoice.invoiceNumber() + " marked PAID");
                return 0;
            }
            case "list-invoices" -> {
                for (Invoice invoice : service.listInvoices()) {
                    System.out.printf("%s  %s  %s  %s%n",
                            invoice.id(), invoice.invoiceNumber(), invoice.status(), invoice.total().format());
                }
                return 0;
            }
            case "show" -> {
                Invoice invoice = service.getInvoice(required(options, "--invoice"));
                printInvoice(invoice, service.getClient(invoice.clientId()));
                return 0;
            }
            case "export-html" -> {
                Invoice invoice = service.getInvoice(required(options, "--invoice"));
                Client client = service.getClient(invoice.clientId());
                Path output = Path.of(required(options, "--out"));
                new HtmlInvoiceExporter().export(invoice, client, output);
                System.out.println("Exported " + output);
                return 0;
            }
            default -> {
                System.err.println("Unknown command: " + args[0]);
                printHelp();
                return 2;
            }
        }
    }

    private static void printInvoice(Invoice invoice, Client client) {
        System.out.println("Invoice " + invoice.invoiceNumber() + " (" + invoice.status() + ")");
        System.out.println("Client: " + client.name() + " <" + client.email() + ">");
        System.out.println("Due: " + invoice.dueDate());
        for (var item : invoice.items()) {
            System.out.printf("- %s x %s @ %s = %s%n",
                    item.quantity().toPlainString(),
                    item.description(),
                    item.unitPrice().format(),
                    item.total().format());
        }
        System.out.println("Subtotal: " + invoice.subtotal().format());
        System.out.println("Tax: " + invoice.tax().format());
        System.out.println("Total: " + invoice.total().format());
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (!key.startsWith("--")) {
                throw new IllegalArgumentException("Expected option, got: " + key);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for option: " + key);
            }
            options.put(key, args[++i]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option: " + name);
        }
        return value;
    }

    private static void printHelp() {
        System.out.println("""
                InvoicePilot commands:
                  add-client --name NAME --email EMAIL [--company COMPANY] [--address ADDRESS]
                  list-clients
                  create-invoice --client CLIENT_ID --number NUMBER --due YYYY-MM-DD [--tax 0.16] [--currency USD]
                  add-item --invoice INVOICE_ID --description TEXT --quantity QTY --price PRICE [--currency USD]
                  mark-sent --invoice INVOICE_ID
                  mark-paid --invoice INVOICE_ID
                  list-invoices
                  show --invoice INVOICE_ID
                  export-html --invoice INVOICE_ID --out exports/invoice.html

                Add --data PATH to use a custom data file.
                """);
    }
}
