[CmdletBinding()]
param(
    [string]$Path = (Join-Path $PSScriptRoot "..\.env.production")
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "Production environment file not found: $Path"
}

$values = @{}
foreach ($line in Get-Content -LiteralPath $Path) {
    if ($line -match '^\s*#' -or [string]::IsNullOrWhiteSpace($line)) { continue }
    if ($line -notmatch '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        throw "Invalid environment line: $line"
    }
    $values[$Matches[1]] = $Matches[2].Trim()
}

$required = @(
    "DATABASE_URL",
    "POSTGRES_USER",
    "POSTGRES_PASSWORD",
    "GITHUB_CLIENT_ID",
    "GITHUB_CLIENT_SECRET",
    "TOKEN_ENCRYPTION_KEY",
    "FRONTEND_URL",
    "CORS_ALLOWED_ORIGINS",
    "NEXT_PUBLIC_API_BASE_URL",
    "ATTESTATION_CONTRACT_ADDRESS",
    "MONITORING_TOKEN",
    "AI_PROVIDER"
)

$missing = @($required | Where-Object { -not $values.ContainsKey($_) -or [string]::IsNullOrWhiteSpace($values[$_]) })
if ($missing.Count -gt 0) {
    throw "Missing production values: $($missing -join ', ')"
}

foreach ($urlKey in @("FRONTEND_URL", "NEXT_PUBLIC_API_BASE_URL")) {
    $uri = $null
    if (-not [Uri]::TryCreate($values[$urlKey], [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne "https") {
        throw "$urlKey must be an absolute HTTPS URL"
    }
}

$allowedOrigins = @($values["CORS_ALLOWED_ORIGINS"].Split(',') | ForEach-Object { $_.Trim() })
if ($allowedOrigins -notcontains $values["FRONTEND_URL"]) {
    throw "CORS_ALLOWED_ORIGINS must include FRONTEND_URL"
}
foreach ($origin in $allowedOrigins) {
    $originUri = $null
    if (-not [Uri]::TryCreate($origin, [UriKind]::Absolute, [ref]$originUri) -or $originUri.Scheme -ne "https") {
        throw "Every CORS_ALLOWED_ORIGINS entry must be an absolute HTTPS URL"
    }
}

if ($values["DATABASE_URL"] -notmatch '^jdbc:postgresql://') {
    throw "DATABASE_URL must be a PostgreSQL JDBC URL"
}

if ($values["ATTESTATION_CONTRACT_ADDRESS"] -notmatch '^0x[0-9a-fA-F]{40}$') {
    throw "ATTESTATION_CONTRACT_ADDRESS must be a valid EVM address"
}

try {
    $keyBytes = [Convert]::FromBase64String($values["TOKEN_ENCRYPTION_KEY"])
} catch {
    throw "TOKEN_ENCRYPTION_KEY must be valid Base64"
}
if ($keyBytes.Length -ne 32) {
    throw "TOKEN_ENCRYPTION_KEY must decode to exactly 32 bytes"
}

if ($values["MONITORING_TOKEN"].Length -lt 32) {
    throw "MONITORING_TOKEN must contain at least 32 characters"
}

if ($values["AI_PROVIDER"] -ne "rule-based") {
    throw "AI_PROVIDER must remain rule-based until a real AI provider is implemented"
}

Write-Host "Production environment validation passed."
