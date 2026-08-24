[CmdletBinding()]
param(
    [string]$Destination = (Join-Path $PSScriptRoot "..\backups"),
    [ValidateRange(1, 3650)]
    [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:POSTGRES_BACKUP_URL)) {
    throw "POSTGRES_BACKUP_URL must contain a PostgreSQL connection URI"
}
if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw "pg_dump is required to create a backup"
}
if (-not (Get-Command pg_restore -ErrorAction SilentlyContinue)) {
    throw "pg_restore is required to verify a backup"
}

$backupDirectory = [IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$stamp = [DateTimeOffset]::UtcNow.ToString("yyyyMMdd-HHmmss")
$backupPath = Join-Path $backupDirectory "contribution-$stamp.dump"

$previousPgDatabase = $env:PGDATABASE
try {
    $env:PGDATABASE = $env:POSTGRES_BACKUP_URL
    & pg_dump --format=custom --compress=9 --no-owner --no-acl --file=$backupPath
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump failed with exit code $LASTEXITCODE"
    }
} finally {
    if ($null -eq $previousPgDatabase) {
        Remove-Item Env:PGDATABASE -ErrorAction SilentlyContinue
    } else {
        $env:PGDATABASE = $previousPgDatabase
    }
}

& pg_restore --list $backupPath | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Backup verification failed for $backupPath"
}

$cutoff = [DateTime]::UtcNow.AddDays(-$RetentionDays)
Get-ChildItem -LiteralPath $backupDirectory -Filter "contribution-*.dump" -File |
    Where-Object { $_.LastWriteTimeUtc -lt $cutoff } |
    ForEach-Object {
        $candidate = [IO.Path]::GetFullPath($_.FullName)
        $expectedPrefix = $backupDirectory.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
        if (-not $candidate.StartsWith($expectedPrefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove backup outside the selected directory: $candidate"
        }
        Remove-Item -LiteralPath $candidate -Force
    }

Write-Host "Verified PostgreSQL backup created: $backupPath"
