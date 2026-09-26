package com.nightjas50.invoicepilot.export;

import com.nightjas50.invoicepilot.domain.Client;
import com.nightjas50.invoicepilot.domain.Invoice;
import com.nightjas50.invoicepilot.domain.LineItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HtmlInvoiceExporter {
    public Path export(Invoice invoice, Client client, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, render(invoice, client), StandardCharsets.UTF_8);
        return outputPath;
    }

    public String render(Invoice invoice, Client client) {
        StringBuilder rows = new StringBuilder();
        for (LineItem item : invoice.items()) {
            rows.append("<tr>")
                    .append("<td>").append(escape(item.description())).append("</td>")
                    .append("<td class=\"num\">").append(item.quantity().toPlainString()).append("</td>")
                    .append("<td class=\"num\">").append(item.unitPrice().format()).append("</td>")
                    .append("<td class=\"num\">").append(item.total().format()).append("</td>")
                    .append("</tr>");
        }
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <title>Invoice %s</title>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 40px; color: #17202a; }
                    header { display: flex; justify-content: space-between; gap: 32px; border-bottom: 2px solid #17202a; padding-bottom: 20px; }
                    h1 { margin: 0; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 32px; }
                    th, td { border-bottom: 1px solid #d6dce2; padding: 10px; text-align: left; }
                    th { background: #f3f6f9; }
                    .num { text-align: right; }
                    .totals { margin-left: auto; width: 320px; }
                    .muted { color: #607080; }
                  </style>
                </head>
                <body>
                  <header>
                    <section>
                      <h1>Invoice %s</h1>
                      <p class="muted">Status: %s</p>
                    </section>
                    <section>
                      <strong>%s</strong><br>
                      %s<br>
                      %s
                    </section>
                  </header>
                  <p><strong>Issue date:</strong> %s<br><strong>Due date:</strong> %s</p>
                  <table>
                    <thead><tr><th>Description</th><th class="num">Qty</th><th class="num">Unit</th><th class="num">Total</th></tr></thead>
                    <tbody>%s</tbody>
                  </table>
                  <table class="totals">
                    <tr><td>Subtotal</td><td class="num">%s</td></tr>
                    <tr><td>Discount</td><td class="num">%s</td></tr>
                    <tr><td>Tax</td><td class="num">%s</td></tr>
                    <tr><th>Total</th><th class="num">%s</th></tr>
                  </table>
                  <p>%s</p>
                </body>
                </html>
                """.formatted(
                escape(invoice.invoiceNumber()),
                escape(invoice.invoiceNumber()),
                invoice.status(),
                escape(client.name()),
                escape(client.email()),
                escape(client.address()),
                invoice.issueDate(),
                invoice.dueDate(),
                rows,
                invoice.subtotal().format(),
                invoice.discount().format(),
                invoice.tax().format(),
                invoice.total().format(),
                escape(invoice.notes())
        );
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
