package com.nightjas50.invoicepilot.web;

import com.nightjas50.invoicepilot.domain.Client;
import com.nightjas50.invoicepilot.domain.Invoice;
import com.nightjas50.invoicepilot.domain.LineItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonUtil {
    private JsonUtil() {
    }

    static String clientJson(Client client) {
        return "{" +
                field("id", client.id()) + "," +
                field("name", client.name()) + "," +
                field("email", client.email()) + "," +
                field("company", client.company()) + "," +
                field("address", client.address()) +
                "}";
    }

    static String invoiceJson(Invoice invoice, Client client) {
        StringBuilder items = new StringBuilder("[");
        for (int i = 0; i < invoice.items().size(); i++) {
            LineItem item = invoice.items().get(i);
            if (i > 0) {
                items.append(",");
            }
            items.append("{")
                    .append(field("description", item.description())).append(",")
                    .append(numberField("quantity", item.quantity().toPlainString())).append(",")
                    .append(field("unitPrice", item.unitPrice().format())).append(",")
                    .append(field("total", item.total().format()))
                    .append("}");
        }
        items.append("]");
        return "{" +
                field("id", invoice.id()) + "," +
                field("invoiceNumber", invoice.invoiceNumber()) + "," +
                field("clientId", invoice.clientId()) + "," +
                field("clientName", client.name()) + "," +
                field("issueDate", invoice.issueDate().toString()) + "," +
                field("dueDate", invoice.dueDate().toString()) + "," +
                field("status", invoice.status().name()) + "," +
                field("subtotal", invoice.subtotal().format()) + "," +
                field("tax", invoice.tax().format()) + "," +
                field("total", invoice.total().format()) + "," +
                "\"items\":" + items +
                "}";
    }

    static String array(List<String> values) {
        return "[" + String.join(",", values) + "]";
    }

    static String field(String name, String value) {
        return quote(name) + ":" + quote(value);
    }

    static String numberField(String name, String value) {
        return quote(name) + ":" + value;
    }

    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(ch);
            }
        }
        return out.append("\"").toString();
    }

    static Map<String, String> parseFlatJson(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) {
            body = body.substring(1);
        }
        if (body.endsWith("}")) {
            body = body.substring(0, body.length() - 1);
        }
        int index = 0;
        while (index < body.length()) {
            index = skipWhitespaceAndComma(body, index);
            if (index >= body.length()) {
                break;
            }
            ParseResult key = readJsonString(body, index);
            index = skipWhitespace(body, key.nextIndex());
            if (index >= body.length() || body.charAt(index) != ':') {
                throw new IllegalArgumentException("Invalid JSON object");
            }
            index = skipWhitespace(body, index + 1);
            ParseResult value;
            if (body.charAt(index) == '"') {
                value = readJsonString(body, index);
                index = value.nextIndex();
            } else {
                int start = index;
                while (index < body.length() && body.charAt(index) != ',') {
                    index++;
                }
                value = new ParseResult(body.substring(start, index).trim(), index);
            }
            values.put(key.value(), value.value());
        }
        return values;
    }

    private static int skipWhitespaceAndComma(String value, int index) {
        while (index < value.length() && (Character.isWhitespace(value.charAt(index)) || value.charAt(index) == ',')) {
            index++;
        }
        return index;
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static ParseResult readJsonString(String value, int index) {
        if (value.charAt(index) != '"') {
            throw new IllegalArgumentException("Expected JSON string");
        }
        StringBuilder out = new StringBuilder();
        index++;
        while (index < value.length()) {
            char ch = value.charAt(index++);
            if (ch == '"') {
                return new ParseResult(out.toString(), index);
            }
            if (ch == '\\' && index < value.length()) {
                char escaped = value.charAt(index++);
                switch (escaped) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    default -> out.append(escaped);
                }
            } else {
                out.append(ch);
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private record ParseResult(String value, int nextIndex) {
    }
}
