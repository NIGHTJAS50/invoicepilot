# InvoicePilot

InvoicePilot is a serious object-oriented Java invoicing MVP for freelancers and small businesses. It runs as a command-line application, stores data locally, calculates invoice totals, tracks status, and exports printable HTML invoices.

The project is intentionally dependency-free so it can build with plain `javac` and `java`.

## Features

- Object-oriented domain model: `Client`, `Invoice`, `LineItem`, `Money`, `InvoiceStatus`
- Local file storage using a simple TSV data file
- Add and list clients
- Create invoices
- Add invoice items
- Calculate subtotal, tax, discount, and total
- Mark invoices sent or paid
- Export printable HTML invoices
- Lightweight Java test runner

## Build

From the project root:

```powershell
.\scripts\build.ps1
```

## Run

```powershell
.\scripts\run.ps1 help
```

## Example Workflow

```powershell
.\scripts\run.ps1 add-client --name "Amina Lee" --email "amina@example.com" --company "Amina Studio"

.\scripts\run.ps1 list-clients

.\scripts\run.ps1 create-invoice --client CLIENT_ID --number INV-001 --due 2030-01-31 --tax 0.16 --currency USD

.\scripts\run.ps1 add-item --invoice INVOICE_ID --description "Website design" --quantity 1 --price 900 --currency USD

.\scripts\run.ps1 mark-sent --invoice INVOICE_ID

.\scripts\run.ps1 show --invoice INVOICE_ID

.\scripts\run.ps1 export-html --invoice INVOICE_ID --out exports/invoice.html
```

Replace `CLIENT_ID` and `INVOICE_ID` with values printed by earlier commands.

## Test

```powershell
.\scripts\test.ps1
```

Expected:

```text
All InvoicePilot tests passed
```

## Architecture

```text
cli/        command-line application
domain/     core OOP business model
service/    use-case layer
storage/    local persistence
export/     printable invoice output
```

## Author

NIGHTJAS50
