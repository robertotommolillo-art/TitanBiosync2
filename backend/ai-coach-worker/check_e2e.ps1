param(
    [string]$ApiUrl = "",
    [string]$AppToken = "",
    [switch]$SkipGenerateWorkout
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-Value {
    param(
        [string]$Explicit,
        [string]$EnvName,
        [scriptblock]$Fallback
    )

    if (-not [string]::IsNullOrWhiteSpace($Explicit)) { return $Explicit.Trim() }

    $envValue = [Environment]::GetEnvironmentVariable($EnvName)
    if (-not [string]::IsNullOrWhiteSpace($envValue)) { return $envValue.Trim() }

    return (& $Fallback)
}

function Read-BuildGradleValue {
    param(
        [string]$RootPath,
        [string]$VariableName
    )

    $gradlePath = Join-Path $RootPath "app\build.gradle.kts"
    if (-not (Test-Path $gradlePath)) { return "" }

    $content = Get-Content $gradlePath -Raw
    $pattern = 'val\s+{0}\s*=\s*"([^"]+)"' -f [regex]::Escape($VariableName)
    $match = [regex]::Match($content, $pattern)
    if ($match.Success) { return $match.Groups[1].Value.Trim() }

    return ""
}

function Invoke-CoachRequest {
    param(
        [string]$Url,
        [string]$Token,
        [string]$Path,
        [string]$Body
    )

    $endpoint = "$($Url.TrimEnd('/'))$Path"
    $headers = @{ "Content-Type" = "application/json" }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["X-App-Token"] = $Token
    }

    try {
        $response = Invoke-WebRequest -Method Post -Uri $endpoint -Headers $headers -Body $Body -UseBasicParsing
        return [pscustomobject]@{
            Ok = $true
            StatusCode = [int]$response.StatusCode
            Body = $response.Content
            Endpoint = $endpoint
        }
    }
    catch {
        $statusCode = 0
        $body = ""

        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $body = $reader.ReadToEnd()
                $reader.Dispose()
            }
            catch {
                $body = $_.Exception.Message
            }
        }
        else {
            $body = $_.Exception.Message
        }

        return [pscustomobject]@{
            Ok = $false
            StatusCode = $statusCode
            Body = $body
            Endpoint = $endpoint
        }
    }
}

$workspaceRoot = Split-Path -Path (Split-Path -Path $PSScriptRoot -Parent) -Parent

$resolvedApiUrl = Resolve-Value -Explicit $ApiUrl -EnvName "COACH_API_URL" -Fallback {
    Read-BuildGradleValue -RootPath $workspaceRoot -VariableName "defaultCoachApiUrl"
}

$resolvedToken = Resolve-Value -Explicit $AppToken -EnvName "COACH_APP_TOKEN" -Fallback {
    Read-BuildGradleValue -RootPath $workspaceRoot -VariableName "defaultCoachAppToken"
}

if ([string]::IsNullOrWhiteSpace($resolvedApiUrl)) {
    Write-Host "ERRORE: URL API non risolta. Passa -ApiUrl oppure imposta COACH_API_URL." -ForegroundColor Red
    exit 2
}

if ([string]::IsNullOrWhiteSpace($resolvedToken)) {
    Write-Host "ERRORE: APP token non risolto. Passa -AppToken oppure imposta COACH_APP_TOKEN." -ForegroundColor Red
    exit 2
}

$chatBody = '{"messages":[{"role":"user","content":"Test auth TitanBiosync: rispondi con OK"}]}'
$workoutBody = '{"userSpec":"scheda full body 3 giorni principiante"}'

Write-Host "=== TitanBiosync Coach E2E Check ===" -ForegroundColor Cyan
Write-Host "API URL: $resolvedApiUrl"
Write-Host "Token: $($resolvedToken.Substring(0, [Math]::Min(6, $resolvedToken.Length)))... (mascherato)"
Write-Host ""

# 1) Negative test: no token must return 401
$unauth = Invoke-CoachRequest -Url $resolvedApiUrl -Token "" -Path "/coach/chat" -Body $chatBody
if ($unauth.StatusCode -ne 401) {
    Write-Host "[FAIL] Test non autorizzato: atteso 401, ottenuto $($unauth.StatusCode)." -ForegroundColor Red
    Write-Host "Endpoint: $($unauth.Endpoint)"
    Write-Host "Body: $($unauth.Body)"
    exit 1
}
Write-Host "[OK] Test non autorizzato: 401 confermato senza token." -ForegroundColor Green

# 2) Positive auth test: token should not return 401
$authChat = Invoke-CoachRequest -Url $resolvedApiUrl -Token $resolvedToken -Path "/coach/chat" -Body $chatBody
if ($authChat.StatusCode -eq 401) {
    Write-Host "[FAIL] Chat autenticata: ancora 401 (token mismatch)." -ForegroundColor Red
    Write-Host "Endpoint: $($authChat.Endpoint)"
    Write-Host "Body: $($authChat.Body)"
    exit 1
}
if (-not ($authChat.StatusCode -ge 200 -and $authChat.StatusCode -le 299)) {
    Write-Host "[FAIL] Chat autenticata: HTTP $($authChat.StatusCode) (atteso 2xx)." -ForegroundColor Red
    Write-Host "Endpoint: $($authChat.Endpoint)"
    Write-Host "Body: $($authChat.Body)"
    exit 1
}
Write-Host "[OK] Chat autenticata: HTTP $($authChat.StatusCode)." -ForegroundColor Green

if (-not $SkipGenerateWorkout) {
    $authWorkout = Invoke-CoachRequest -Url $resolvedApiUrl -Token $resolvedToken -Path "/coach/generate-workout" -Body $workoutBody
    if ($authWorkout.StatusCode -eq 401) {
        Write-Host "[FAIL] Generate workout autenticata: ancora 401 (token mismatch)." -ForegroundColor Red
        Write-Host "Endpoint: $($authWorkout.Endpoint)"
        Write-Host "Body: $($authWorkout.Body)"
        exit 1
    }
    if (-not ($authWorkout.StatusCode -ge 200 -and $authWorkout.StatusCode -le 299)) {
        Write-Host "[FAIL] Generate workout autenticata: HTTP $($authWorkout.StatusCode) (atteso 2xx)." -ForegroundColor Red
        Write-Host "Endpoint: $($authWorkout.Endpoint)"
        Write-Host "Body: $($authWorkout.Body)"
        exit 1
    }
    Write-Host "[OK] Generate workout autenticata: HTTP $($authWorkout.StatusCode)." -ForegroundColor Green
}

Write-Host ""
Write-Host "E2E completato: autenticazione OK e richieste AI operative." -ForegroundColor Cyan
exit 0

