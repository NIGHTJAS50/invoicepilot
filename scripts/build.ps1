$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path out/classes | Out-Null
javac -d out/classes (Get-ChildItem -Recurse src/main/java/*.java).FullName
if (Test-Path src/main/resources) {
  Copy-Item -Recurse -Force src/main/resources/* out/classes/
}
Write-Host "Build complete: out/classes"
