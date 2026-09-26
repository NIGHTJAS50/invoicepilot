$ErrorActionPreference = "Stop"
if (-not (Test-Path out/classes)) {
  & "$PSScriptRoot\build.ps1"
}
java -cp out/classes com.nightjas50.invoicepilot.cli.InvoicePilotApp @args
