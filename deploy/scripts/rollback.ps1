param(
    [Parameter(Mandatory = $true)]
    [string]$PreviousTag,
    [string]$ProjectName = "wjys",
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "找不到环境文件: $EnvFile"
}

$env:IMAGE_TAG = $PreviousTag
docker compose --env-file $EnvFile -p $ProjectName up -d --no-build backend frontend
if ($LASTEXITCODE -ne 0) {
    throw "回滚启动失败，镜像标签: $PreviousTag"
}

Write-Output "已切换到镜像标签: $PreviousTag"
