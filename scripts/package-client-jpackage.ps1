param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [switch]$SkipClean,
    [string]$JpackageJdkHome = $env:JPACKAGE_JDK_HOME,
    [string]$AppName = "MCFlashLinkClient",
    [string]$ServiceName = "MCFlashLinkClient",
    [string]$ServiceDisplayName = "MC FlashLink Client"
)

Set-Location $ProjectRoot

function Find-Jpackage {
    param(
        [string]$JdkHome
    )

    if ($JdkHome) {
        $candidate = Join-Path $JdkHome "bin\jpackage.exe"
        if (Test-Path -LiteralPath $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    $command = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidateRoots = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Amazon Corretto"
    )

    foreach ($candidateRoot in $candidateRoots) {
        if (-not (Test-Path -LiteralPath $candidateRoot)) {
            continue
        }
        $candidate = Get-ChildItem -Path $candidateRoot -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match 'jdk|temurin|corretto|msopenjdk' } |
                Sort-Object Name -Descending |
                ForEach-Object { Join-Path $_.FullName "bin\jpackage.exe" } |
                Where-Object { Test-Path -LiteralPath $_ } |
                Select-Object -First 1
        if ($candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    return $null
}

$jpackageExe = Find-Jpackage -JdkHome $JpackageJdkHome
if (-not $jpackageExe) {
    Write-Error "jpackage.exe not found. Install JDK 17+ and set JPACKAGE_JDK_HOME to that JDK path, for example: C:\Program Files\Java\jdk-17"
    exit 1
}

$mavenArgs = @("-pl", "client", "-am")
if (-not $SkipClean) {
    $mavenArgs += "clean"
}
$mavenArgs += @("package", "-DskipTests")
& mvn @mavenArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$clientJarName = "client-0.1.0-SNAPSHOT.jar"
$clientJar = Join-Path $ProjectRoot "client\target\$clientJarName"
$jpackageRoot = Join-Path $ProjectRoot "client\target\jpackage"
$inputDir = Join-Path $jpackageRoot "input"
$launcherDir = Join-Path $jpackageRoot "launchers"
$imageDir = Join-Path $jpackageRoot "image"
$appDir = Join-Path $imageDir $AppName

if (Test-Path -LiteralPath $jpackageRoot) {
    Remove-Item -LiteralPath $jpackageRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
New-Item -ItemType Directory -Force -Path $launcherDir | Out-Null
New-Item -ItemType Directory -Force -Path $imageDir | Out-Null

Copy-Item -LiteralPath $clientJar -Destination (Join-Path $inputDir $clientJarName)

$serviceLauncher = Join-Path $launcherDir "$($AppName)Service.properties"
$serviceLauncherText = @(
    "arguments=--service --mc-flash-link.client.open-browser=false"
)
Set-Content -Encoding ASCII -Path $serviceLauncher -Value $serviceLauncherText

& $jpackageExe `
    --type app-image `
    --name $AppName `
    --app-version "0.1.0" `
    --vendor "MC-FlashLink" `
    --input $inputDir `
    --dest $imageDir `
    --main-jar $clientJarName `
    --java-options "-Dfile.encoding=UTF-8" `
    --add-launcher "$($AppName)Service=$serviceLauncher" `
    --win-console

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$installService = @"
param(
    [string]`$ServiceName = "$ServiceName",
    [string]`$DisplayName = "$ServiceDisplayName"
)

`$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not `$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File ```"`$PSCommandPath```""
    exit
}

`$appDir = Split-Path -Parent `$PSCommandPath
`$serviceExe = Join-Path `$appDir "$($AppName)Service.exe"
if (-not (Test-Path -LiteralPath `$serviceExe)) {
    Write-Error "Service executable not found: `$serviceExe"
    exit 1
}

`$existing = Get-Service -Name `$ServiceName -ErrorAction SilentlyContinue
if (`$existing) {
    if (`$existing.Status -ne 'Stopped') {
        Stop-Service -Name `$ServiceName -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
    sc.exe delete `$ServiceName | Out-Null
    Start-Sleep -Seconds 2
}

`$binPath = '"' + `$serviceExe + '"'
sc.exe create `$ServiceName binPath= `$binPath start= auto DisplayName= "`$DisplayName" | Out-Null
if (`$LASTEXITCODE -ne 0) {
    Write-Error "Failed to create Windows service: `$ServiceName"
    exit `$LASTEXITCODE
}

sc.exe description `$ServiceName "MC-FlashLink local client service. Web console: http://127.0.0.1:26333" | Out-Null
sc.exe failure `$ServiceName reset= 60 actions= restart/5000/restart/10000/""/0 | Out-Null
Start-Service -Name `$ServiceName
Write-Host "Service installed and started: `$ServiceName"
Write-Host "Web console: http://127.0.0.1:26333"
"@
Set-Content -Encoding UTF8 -Path (Join-Path $appDir "install-service.ps1") -Value $installService

$uninstallService = @"
param(
    [string]`$ServiceName = "$ServiceName"
)

`$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not `$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File ```"`$PSCommandPath```""
    exit
}

`$existing = Get-Service -Name `$ServiceName -ErrorAction SilentlyContinue
if (-not `$existing) {
    Write-Host "Service not installed: `$ServiceName"
    exit 0
}

if (`$existing.Status -ne 'Stopped') {
    Stop-Service -Name `$ServiceName -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}
sc.exe delete `$ServiceName | Out-Null
Write-Host "Service uninstalled: `$ServiceName"
"@
Set-Content -Encoding UTF8 -Path (Join-Path $appDir "uninstall-service.ps1") -Value $uninstallService

$startService = @"
`$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not `$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -Command Start-Service -Name '$ServiceName'"
    exit
}
Start-Service -Name "$ServiceName"
"@
Set-Content -Encoding UTF8 -Path (Join-Path $appDir "start-service.ps1") -Value $startService

$stopService = @"
`$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not `$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -Command Stop-Service -Name '$ServiceName' -Force"
    exit
}
Stop-Service -Name "$ServiceName" -Force
"@
Set-Content -Encoding UTF8 -Path (Join-Path $appDir "stop-service.ps1") -Value $stopService

$runClientCmd = @(
    '@echo off',
    'setlocal',
    'set "APP_DIR=%~dp0"',
    'powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Start-Process ''%APP_DIR%MCFlashLinkClient.exe'' -Verb RunAs"'
)
Set-Content -Encoding ASCII -Path (Join-Path $appDir "run-client-admin.cmd") -Value $runClientCmd

$installServiceCmd = @(
    '@echo off',
    'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-service.ps1"'
)
Set-Content -Encoding ASCII -Path (Join-Path $appDir "install-service.cmd") -Value $installServiceCmd

$uninstallServiceCmd = @(
    '@echo off',
    'powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0uninstall-service.ps1"'
)
Set-Content -Encoding ASCII -Path (Join-Path $appDir "uninstall-service.cmd") -Value $uninstallServiceCmd

$zipPath = Join-Path $ProjectRoot "client\target\$AppName-windows.zip"
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
Compress-Archive -Path (Join-Path $appDir "*") -DestinationPath $zipPath

Write-Host "jpackage executable:"
Write-Host $jpackageExe
Write-Host "Windows app image:"
Write-Host $appDir
Write-Host "Windows app zip:"
Write-Host $zipPath
Write-Host "Interactive executable:"
Write-Host (Join-Path $appDir "$AppName.exe")
Write-Host "Windows service installer:"
Write-Host (Join-Path $appDir "install-service.cmd")
