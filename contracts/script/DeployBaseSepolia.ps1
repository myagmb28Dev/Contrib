param(
    [string]$Account = "contrib-deployer",
    [string]$RpcUrl = "https://base-sepolia-rpc.publicnode.com"
)

$ErrorActionPreference = "Stop"
$forge = Join-Path $env:USERPROFILE ".foundry\bin\forge.exe"
$cast = Join-Path $env:USERPROFILE ".foundry\bin\cast.exe"
$keystore = Join-Path $env:USERPROFILE ".foundry\keystores\$Account"

if (-not (Test-Path -LiteralPath $forge) -or -not (Test-Path -LiteralPath $cast)) {
    throw "Foundry is not installed in $env:USERPROFILE\.foundry\bin"
}

if (-not (Test-Path -LiteralPath $keystore)) {
    throw "Encrypted keystore not found: $keystore"
}

$chainId = (& $cast chain-id --rpc-url $RpcUrl).Trim()
if ($LASTEXITCODE -ne 0 -or $chainId -ne "84532") {
    throw "RPC endpoint is not Base Sepolia (expected chain ID 84532, got $chainId)"
}

& $forge create `
    "src/ContributionAttestation.sol:ContributionAttestation" `
    --rpc-url $RpcUrl `
    --keystore $keystore `
    --broadcast

if ($LASTEXITCODE -ne 0) {
    throw "Base Sepolia deployment failed"
}
