param(
    [switch]$SkipTests,
    [switch]$SkipInstall
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot ".")).Path
$maven = Join-Path $root "mvnw.cmd"
$harnessCheck = Join-Path $root "scripts\check-harness.ps1"

Write-Host "=== AI Travel Planner Harness ==="
Write-Host "Repository: $root"
Write-Host "Java:"
java -version

if (-not (Test-Path $maven)) {
    throw "Maven wrapper not found: $maven"
}

if (-not $SkipInstall) {
    Write-Host ""
    Write-Host "=== Resolving Maven dependencies ==="
    & $maven -q -DskipTests dependency:go-offline
    if ($LASTEXITCODE -ne 0) {
        throw "Dependency resolution failed with exit code $LASTEXITCODE"
    }
}

Write-Host ""
Write-Host "=== Validating harness state ==="
& powershell -NoProfile -ExecutionPolicy Bypass -File $harnessCheck
if ($LASTEXITCODE -ne 0) {
    throw "Harness validation failed with exit code $LASTEXITCODE"
}

if (-not $SkipTests) {
    Write-Host ""
    Write-Host "=== Running Maven tests ==="
    & $maven test
    if ($LASTEXITCODE -ne 0) {
        throw @"
Maven tests failed with exit code $LASTEXITCODE.
Fix the baseline before starting a new feature.
Record the failure in claude-progress.md and feature_list.json.
"@
    }
}

Write-Host ""
Write-Host "=== Standard start ==="
Write-Host "Preferred: run AiTravelPlannerApplication in IDEA with JDK 26."
Write-Host "Backend: http://localhost:8123/api"
Write-Host "Use the documented WMI launcher only when IDEA is unavailable."
Write-Host "Never place API keys in a tracked script or log."
