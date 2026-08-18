$ErrorActionPreference = "Stop"
$cast = Join-Path $env:USERPROFILE ".foundry\bin\cast.exe"
$keystoreDirectory = Join-Path $env:USERPROFILE ".foundry\keystores"
$accountName = "contrib-deployer"
$keystorePath = Join-Path $keystoreDirectory $accountName

if (-not (Test-Path -LiteralPath $cast)) {
    throw "Foundry cast is not installed in $env:USERPROFILE\.foundry\bin"
}

if (Test-Path -LiteralPath $keystorePath) {
    throw "The $accountName keystore already exists. It was not overwritten."
}

New-Item -ItemType Directory -Path $keystoreDirectory -Force | Out-Null
& $cast wallet new $keystoreDirectory $accountName
if ($LASTEXITCODE -ne 0) {
    throw "Wallet creation failed"
}

Write-Host ""
Write-Host "테스트 지갑 생성 완료! 이 창은 닫아도 돼." -ForegroundColor Green
Write-Host "키스토어 파일: $keystorePath"
