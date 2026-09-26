# Architecture

InvoicePilot uses a layered object-oriented design.

## Domain Layer

The domain layer owns the business rules:

- `Money` handles currency-safe arithmetic.
- `Client` represents invoice recipients.
- `LineItem` validates quantity and unit price.
- `Invoice` calculates subtotal, tax, and total, and controls state transitions.
- `InvoiceStatus` models invoice lifecycle state.

## Service Layer

`InvoiceService` coordinates use cases such as creating invoices, adding items, and marking invoices paid. It keeps the CLI from directly manipulating storage internals.

## Storage Layer

`InvoicePilotStore` persists clients and invoices to a local TSV data file. The format is simple and inspectable, while the class hides parsing and serialization from the rest of the app.

## Export Layer

`HtmlInvoiceExporter` turns invoice objects into printable HTML. A future version could add PDF export or email delivery without changing the domain model.

## CLI Layer

`InvoicePilotApp` parses commands and delegates to services. This keeps the command-line interface thin.

## Web Layer

`InvoicePilotWebServer` uses the JDK's built-in HTTP server. It serves the static browser interface and exposes a JSON API for clients, invoices, line items, status transitions, summary metrics, and invoice export.

The browser UI lives in `src/main/resources/public` and stays focused on workflow: creating clients, creating invoices, adding line items, marking invoices sent or paid, and opening printable invoice output.
