param(
    [string]$Account = "contrib-deployer",
    [string]$Amount = "0.02ether",
    [string]$RpcUrl = "https://ethereum-sepolia-rpc.publicnode.com"
)

$ErrorActionPreference = "Stop"
$cast = Join-Path $env:USERPROFILE ".foundry\bin\cast.exe"
$keystore = Join-Path $env:USERPROFILE ".foundry\keystores\$Account"
$bridge = "0xfd0Bf71F60660E2f608ed56e1659C450eB113120"

if (-not (Test-Path -LiteralPath $cast)) {
    throw "Foundry is not installed in $env:USERPROFILE\.foundry\bin"
}

if (-not (Test-Path -LiteralPath $keystore)) {
    throw "Encrypted keystore not found: $keystore"
}

$chainId = (& $cast chain-id --rpc-url $RpcUrl).Trim()
if ($LASTEXITCODE -ne 0 -or $chainId -ne "11155111") {
    throw "RPC endpoint is not Ethereum Sepolia (expected chain ID 11155111, got $chainId)"
}

& $cast send `
    $bridge `
    "bridgeETH(uint32,bytes)" `
    200000 `
    0x `
    --value $Amount `
    --rpc-url $RpcUrl `
    --keystore $keystore

if ($LASTEXITCODE -ne 0) {
    throw "Bridge transaction failed"
}

Write-Host "Bridge transaction submitted. Base Sepolia credit can take a few minutes."
