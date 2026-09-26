javac -d out/classes (Get-ChildItem -Recurse src/main/java/*.java).FullName
java -cp out/classes com.nightjas50.invoicepilot.cli.InvoicePilotApp add-client --name "Amina Lee" --email "amina@example.com" --company "Amina Studio"
java -cp out/classes com.nightjas50.invoicepilot.cli.InvoicePilotApp list-clients
