param(
    [string]$BaseUrl = "http://localhost",
    [string]$BackendUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Test-Endpoint([string]$Url) {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 10
    if ($response.StatusCode -ne 200) {
        throw "健康检查失败: $Url 返回 HTTP $($response.StatusCode)"
    }
    return $response
}

$frontend = Test-Endpoint "$BaseUrl/"
$backend = Test-Endpoint "$BackendUrl/actuator/health"
$health = $backend.Content | ConvertFrom-Json
if ($health.status -ne "UP") {
    throw "后端健康状态不是 UP: $($health.status)"
}

Write-Output "frontend: HTTP $($frontend.StatusCode)"
Write-Output "backend: $($health.status)"
