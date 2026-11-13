param(
    [string]$Root = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
} else {
    $Root = (Resolve-Path $Root).Path
}

$errors = [System.Collections.Generic.List[string]]::new()
$warnings = [System.Collections.Generic.List[string]]::new()

$requiredFiles = @(
    "AGENTS.md",
    "ARCHITECTURE.md",
    "feature_list.json",
    "claude-progress.md",
    "session-handoff.md",
    "clean-state-checklist.md",
    "evaluator-rubric.md",
    "quality-document.md",
    "docs\DESIGN.md",
    "docs\FRONTEND.md",
    "docs\PLANS.md",
    "docs\PRODUCT_SENSE.md",
    "docs\QUALITY_SCORE.md",
    "docs\RELIABILITY.md",
    "docs\SECURITY.md"
)

foreach ($relativePath in $requiredFiles) {
    $fullPath = Join-Path $Root $relativePath
    if (-not (Test-Path $fullPath)) {
        $errors.Add("Missing required file: $relativePath")
    }
}

$featurePath = Join-Path $Root "feature_list.json"
if (Test-Path $featurePath) {
    try {
        $featureData = Get-Content -Raw -LiteralPath $featurePath | ConvertFrom-Json
        $features = @($featureData.features)
        $validStatuses = @("not_started", "in_progress", "blocked", "passing")
        $inProgress = @($features | Where-Object { $_.status -eq "in_progress" })

        if ($features.Count -eq 0) {
            $errors.Add("feature_list.json contains no features")
        }
        if ($inProgress.Count -gt 1) {
            $errors.Add("More than one feature is in_progress: $($inProgress.id -join ', ')")
        }

        $ids = @($features | ForEach-Object { $_.id })
        $statusById = @{}
        foreach ($feature in $features) {
            $statusById[$feature.id] = $feature.status
        }
        $duplicateIds = @($ids | Group-Object | Where-Object { $_.Count -gt 1 } | ForEach-Object { $_.Name })
        if ($duplicateIds.Count -gt 0) {
            $errors.Add("Duplicate feature IDs: $($duplicateIds -join ', ')")
        }

        foreach ($feature in $features) {
            if ($feature.id -notmatch '^F-\d{3}$') {
                $errors.Add("Invalid feature ID: $($feature.id)")
            }
            if ($validStatuses -notcontains $feature.status) {
                $errors.Add("Invalid status for $($feature.id): $($feature.status)")
            }
            $dependencyProperty = $feature.PSObject.Properties["dependencies"]
            if ($null -eq $dependencyProperty) {
                $errors.Add("Feature $($feature.id) has no dependencies array")
                $dependencies = @()
            } else {
                $dependencies = @($dependencyProperty.Value)
            }
            foreach ($dependency in $dependencies) {
                if ($dependency -eq $feature.id) {
                    $errors.Add("Feature $($feature.id) depends on itself")
                } elseif ($ids -notcontains $dependency) {
                    $errors.Add("Feature $($feature.id) has unknown dependency: $dependency")
                } elseif ($feature.status -eq "in_progress" -and $statusById[$dependency] -ne "passing") {
                    $errors.Add("In-progress feature $($feature.id) has unfinished dependency: $dependency")
                }
            }
            if (@($feature.verification | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count -eq 0) {
                $errors.Add("Feature $($feature.id) has no verification steps")
            }
            if ($feature.status -eq "passing" -and [string]::IsNullOrWhiteSpace($feature.evidence)) {
                $errors.Add("Passing feature $($feature.id) has no evidence")
            }
            if ($feature.status -eq "blocked" -and [string]::IsNullOrWhiteSpace($feature.notes)) {
                $errors.Add("Blocked feature $($feature.id) has no blocker note")
            }
        }
    } catch {
        $errors.Add("feature_list.json is invalid JSON: $($_.Exception.Message)")
    }
}

$agentsPath = Join-Path $Root "AGENTS.md"
if (Test-Path $agentsPath) {
    $agentsLength = (Get-Item -LiteralPath $agentsPath).Length
    if ($agentsLength -gt 12000) {
        $warnings.Add("AGENTS.md is $agentsLength bytes; move deep rules into docs/")
    }
}

if (-not (Test-Path (Join-Path $Root "docs\exec-plans\active\index.md"))) {
    $warnings.Add("docs/exec-plans/active/index.md is missing")
}
if (-not (Test-Path (Join-Path $Root "docs\exec-plans\completed\index.md"))) {
    $warnings.Add("docs/exec-plans/completed/index.md is missing")
}

Write-Host "Harness root: $Root"
Write-Host "Required files: $($requiredFiles.Count)"
Write-Host "Warnings: $($warnings.Count)"
foreach ($warning in $warnings) {
    Write-Host "WARN $warning"
}

if ($errors.Count -gt 0) {
    Write-Host "Errors: $($errors.Count)"
    foreach ($item in $errors) {
        Write-Host "ERROR $item"
    }
    exit 1
}

Write-Host "Harness validation passed."
