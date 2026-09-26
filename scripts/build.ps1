$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path out/classes | Out-Null
javac -d out/classes (Get-ChildItem -Recurse src/main/java/*.java).FullName
Write-Host "Build complete: out/classes"
