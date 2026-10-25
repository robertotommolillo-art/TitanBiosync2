param(
    [string]$ApiUrl = "",
    [string]$AppToken = "",
    [int]$TokenLength = 48,
    [switch]$SkipWrangler,
    [switch]$SkipE2E,
    [switch]$SkipGradleUserProps,
    [switch]$DryRun
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

function New-SecureToken {
    param([int]$Length)

    if ($Length -lt 24) {
        throw "TokenLength troppo corto: usa almeno 24 caratteri."
    }

    $bytes = New-Object byte[] ($Length)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    # URL-safe Base64 token.
    $token = [Convert]::ToBase64String($bytes).TrimEnd('=')
    $token = $token.Replace('+', '-').Replace('/', '_')
    return $token
}

function Set-PropertyLine {
    param(
        [string[]]$Lines,
        [string]$Key,
        [string]$Value
    )

    $list = New-Object System.Collections.Generic.List[string]
    if ($null -ne $Lines) {
        foreach ($line in @($Lines)) {
            if ($null -ne $line) {
                $list.Add([string]$line)
            }
        }
    }

    $escapedKey = [regex]::Escape($Key)
    $pattern = "^\s*$escapedKey\s*=.*$"
    $replacement = "$Key=$Value"

    for ($i = 0; $i -lt $list.Count; $i++) {
        if ($list[$i] -match $pattern) {
            $list[$i] = $replacement
            return @($list.ToArray())
        }
    }

    $list.Add($replacement)
    return @($list.ToArray())
}

function Save-GradleUserProperties {
    param(
        [string]$ApiUrl,
        [string]$Token,
        [switch]$DryRun
    )

    $gradleUserHome = Join-Path $env:USERPROFILE ".gradle"
    $propsPath = Join-Path $gradleUserHome "gradle.properties"

    if ($DryRun) {
        Write-Host "[DRY-RUN] Aggiornerei $propsPath con COACH_API_URL e COACH_APP_TOKEN"
        return
    }

    if (-not (Test-Path $gradleUserHome)) {
        New-Item -Path $gradleUserHome -ItemType Directory -Force | Out-Null
    }

    $lines = if (Test-Path $propsPath) {
        Get-Content $propsPath
    } else {
        @()
    }

    $lines = Set-PropertyLine -Lines $lines -Key "COACH_API_URL" -Value $ApiUrl
    $lines = Set-PropertyLine -Lines $lines -Key "COACH_APP_TOKEN" -Value $Token

    Set-Content -Path $propsPath -Value $lines -Encoding UTF8
    Write-Host "[OK] Aggiornato $propsPath" -ForegroundColor Green
}

$workspaceRoot = Split-Path -Path (Split-Path -Path $PSScriptRoot -Parent) -Parent
$checkScriptPath = Join-Path $PSScriptRoot "check_e2e.ps1"

$resolvedApiUrl = Resolve-Value -Explicit $ApiUrl -EnvName "COACH_API_URL" -Fallback {
    Read-BuildGradleValue -RootPath $workspaceRoot -VariableName "defaultCoachApiUrl"
}

if ([string]::IsNullOrWhiteSpace($resolvedApiUrl)) {
    throw "API URL non risolta. Passa -ApiUrl oppure imposta COACH_API_URL."
}

$resolvedToken = if (-not [string]::IsNullOrWhiteSpace($AppToken)) {
    $AppToken.Trim()
} else {
    New-SecureToken -Length $TokenLength
}

if ([string]::IsNullOrWhiteSpace($resolvedToken)) {
    throw "Token vuoto: impossibile continuare."
}

$maskedToken = "{0}...{1}" -f $resolvedToken.Substring(0, [Math]::Min(6, $resolvedToken.Length)), $resolvedToken.Substring([Math]::Max(0, $resolvedToken.Length - 4))

Write-Host "=== TitanBiosync Coach Token Setup ===" -ForegroundColor Cyan
Write-Host "API URL: $resolvedApiUrl"
Write-Host "Token (mascherato): $maskedToken"
Write-Host ""
Write-Host "TOKEN COMPLETO (copialo in un posto sicuro):" -ForegroundColor Yellow
Write-Host $resolvedToken -ForegroundColor Yellow
Write-Host ""

if (-not $SkipGradleUserProps) {
    Save-GradleUserProperties -ApiUrl $resolvedApiUrl -Token $resolvedToken -DryRun:$DryRun
}
else {
    Write-Host "[SKIP] Aggiornamento C:\Users\user\.gradle\gradle.properties saltato (-SkipGradleUserProps)." -ForegroundColor DarkYellow
}

if (-not $SkipWrangler) {
    if (-not (Get-Command npx -ErrorAction SilentlyContinue)) {
        throw "npx non trovato. Installa Node.js oppure usa -SkipWrangler."
    }

    Push-Location $PSScriptRoot
    try {
        Write-Host "[1/2] Aggiorno secret APP_TOKEN su Cloudflare Worker..." -ForegroundColor Cyan
        if ($DryRun) {
            Write-Host "[DRY-RUN] npx wrangler secret put APP_TOKEN  # input da file temporaneo senza newline"
        }
        else {
            $tempTokenPath = Join-Path $env:TEMP ("coach-app-token-{0}.txt" -f [Guid]::NewGuid().ToString("N"))
            try {
                # ASCII + NoNewline evita BOM/newline nel secret.
                Set-Content -Path $tempTokenPath -Value $resolvedToken -Encoding ASCII -NoNewline
                $cmd = "npx wrangler secret put APP_TOKEN < `"$tempTokenPath`""
                cmd /c $cmd | Out-Host
                if ($LASTEXITCODE -ne 0) {
                    throw "wrangler secret put APP_TOKEN fallito (exit code $LASTEXITCODE)."
                }
            }
            finally {
                if (Test-Path $tempTokenPath) {
                    Remove-Item $tempTokenPath -Force -ErrorAction SilentlyContinue
                }
            }
        }
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "[SKIP] Aggiornamento secret APP_TOKEN saltato (-SkipWrangler)." -ForegroundColor DarkYellow
}

if (-not $SkipE2E) {
    if (-not (Test-Path $checkScriptPath)) {
        throw "Script E2E non trovato: $checkScriptPath"
    }

    Write-Host "[2/2] Lancio check E2E autenticazione..." -ForegroundColor Cyan
    $cmd = "powershell -ExecutionPolicy Bypass -File `"$checkScriptPath`" -ApiUrl `"$resolvedApiUrl`" -AppToken `"$resolvedToken`""
    if ($DryRun) {
        Write-Host "[DRY-RUN] $cmd"
    }
    else {
        & powershell -ExecutionPolicy Bypass -File $checkScriptPath -ApiUrl $resolvedApiUrl -AppToken $resolvedToken
        if ($LASTEXITCODE -ne 0) {
            throw "Check E2E fallito (exit code $LASTEXITCODE)."
        }
    }
}
else {
    Write-Host "[SKIP] Check E2E saltato (-SkipE2E)." -ForegroundColor DarkYellow
}


Write-Host ""
Write-Host "Completato. Valori da usare lato app:" -ForegroundColor Green
Write-Host "COACH_API_URL=$resolvedApiUrl"
Write-Host "COACH_APP_TOKEN=$resolvedToken"

