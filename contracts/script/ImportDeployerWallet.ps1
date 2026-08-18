$ErrorActionPreference = "Stop"
$cast = Join-Path $env:USERPROFILE ".foundry\bin\cast.exe"

if (-not (Test-Path -LiteralPath $cast)) {
    throw "Foundry cast is not installed in $env:USERPROFILE\.foundry\bin"
}

& $cast wallet import contrib-deployer --interactive
if ($LASTEXITCODE -ne 0) {
    throw "Keystore import failed"
}

Write-Host ""
Write-Host "키스토어 등록 완료! 이 창은 닫아도 돼." -ForegroundColor Green
