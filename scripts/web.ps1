$ErrorActionPreference = "Stop"
if (-not (Test-Path out/classes)) {
  & "$PSScriptRoot\build.ps1"
}
$port = if ($args.Count -ge 1) { $args[0] } else { "8080" }
java -cp out/classes com.nightjas50.invoicepilot.web.InvoicePilotWebServer $port invoicepilot-data.tsv src/main/resources/public
