$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path out/test-classes | Out-Null
javac -d out/test-classes (Get-ChildItem -Recurse src/main/java/*.java,src/test/java/*.java).FullName
if (Test-Path src/main/resources) {
  Copy-Item -Recurse -Force src/main/resources/* out/test-classes/
}
java -cp out/test-classes com.nightjas50.invoicepilot.InvoicePilotTests
