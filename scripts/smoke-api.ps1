param(
    [string]$BaseUrl = $(if ($env:SMOKE_BASE_URL) { $env:SMOKE_BASE_URL } else { 'http://127.0.0.1:8080/dev-api' })
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:BOOTSTRAP_ADMIN_USERNAME) -or
    [string]::IsNullOrWhiteSpace($env:BOOTSTRAP_ADMIN_PASSWORD)) {
    throw '请先设置 BOOTSTRAP_ADMIN_USERNAME 和 BOOTSTRAP_ADMIN_PASSWORD 环境变量。'
}

function Invoke-ApiJson {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][hashtable]$Headers
    )

    $response = Invoke-WebRequest -Uri ($BaseUrl.TrimEnd('/') + $Path) -Method Get `
        -Headers $Headers -SkipHttpErrorCheck
    $payload = $response.Content | ConvertFrom-Json
    if ([int]$response.StatusCode -ne 200 -or $payload.code -ne 200) {
        throw "$Path 请求失败：HTTP $($response.StatusCode)，业务码 $($payload.code)，消息 $($payload.msg)"
    }
    Write-Host "$Path -> HTTP $($response.StatusCode), code $($payload.code)"
    return $payload
}

$loginBody = @{
    username = $env:BOOTSTRAP_ADMIN_USERNAME
    password = $env:BOOTSTRAP_ADMIN_PASSWORD
} | ConvertTo-Json
$login = Invoke-WebRequest -Uri ($BaseUrl.TrimEnd('/') + '/auth/login') -Method Post `
    -ContentType 'application/json' -Body $loginBody -SkipHttpErrorCheck
$loginPayload = $login.Content | ConvertFrom-Json
$token = [string]$login.Headers['satoken'][0]
if ([int]$login.StatusCode -ne 200 -or $loginPayload.code -ne 200 -or [string]::IsNullOrWhiteSpace($token)) {
    throw "登录失败：HTTP $($login.StatusCode)，业务码 $($loginPayload.code)，消息 $($loginPayload.msg)"
}
Write-Output "POST /auth/login -> HTTP $($login.StatusCode), code $($loginPayload.code), satoken 已返回"

$headers = @{ satoken = $token }
foreach ($path in @(
        '/auth/is-login',
        '/auth/me',
        '/departments',
        '/ticket-categories',
        '/admin/departments?page=1&pageSize=10',
        '/admin/users?page=1&pageSize=10',
        '/admin/roles?page=1&pageSize=10',
        '/admin/permissions')) {
    [void](Invoke-ApiJson -Path $path -Headers $headers)
}

$openApi = Invoke-WebRequest -Uri ($BaseUrl.TrimEnd('/') + '/v3/api-docs') -Method Get -SkipHttpErrorCheck
$openApiPayload = $openApi.Content | ConvertFrom-Json
if ([int]$openApi.StatusCode -ne 200 -or $null -eq $openApiPayload.paths -or
    $null -eq $openApiPayload.components.securitySchemes.satoken) {
    throw 'OpenAPI 文档未返回路径或 satoken 安全方案。'
}
Write-Output "GET /v3/api-docs -> HTTP $($openApi.StatusCode), satoken 安全方案已配置"
Write-Output 'API smoke test passed.'
