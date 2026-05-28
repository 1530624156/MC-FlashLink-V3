param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [switch]$SkipClean,
    [string]$BundledJdkHome = $env:JAVA_HOME,
    [switch]$NoBundledJdk
)

Set-Location $ProjectRoot

function Remove-JavaFxRuntimeFiles {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RuntimeRoot
    )

    if (-not (Test-Path $RuntimeRoot)) {
        return
    }

    $resolvedRuntimeRoot = (Resolve-Path $RuntimeRoot).Path
    $javaFxFiles = @(
        "THIRDPARTYLICENSEREADME-JAVAFX.txt",
        "bin\decora_sse.dll",
        "bin\fxplugins.dll",
        "bin\glass.dll",
        "bin\glib-lite.dll",
        "bin\gstreamer-lite.dll",
        "bin\javafx_font.dll",
        "bin\javafx_font_t2k.dll",
        "bin\javafx_iio.dll",
        "bin\jfxmedia.dll",
        "bin\jfxwebkit.dll",
        "bin\prism_common.dll",
        "bin\prism_d3d.dll",
        "bin\prism_sw.dll",
        "lib\ext\jfxrt.jar",
        "lib\javafx.properties"
    )

    foreach ($relativeFile in $javaFxFiles) {
        $target = Join-Path $resolvedRuntimeRoot $relativeFile
        if (Test-Path -LiteralPath $target) {
            Remove-Item -LiteralPath $target -Force
        }
    }
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

$clientJar = Join-Path $ProjectRoot "client\target\client-0.1.0-SNAPSHOT.jar"
$distDir = Join-Path $ProjectRoot "client\target\dist"
$runtimeDir = Join-Path $distDir "runtime"

if (Test-Path $distDir) {
    Remove-Item -LiteralPath $distDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

$javaHome = $BundledJdkHome
if (-not $javaHome) {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $javaHome = (Resolve-Path (Join-Path (Split-Path -Parent $javaCommand.Source) "..")).Path
    }
}
if ($javaHome) {
    $javaHome = (Resolve-Path $javaHome).Path
}

if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
    Write-Error "JDK 8 not found. Set JAVA_HOME to JDK 1.8.0_202 or pass -BundledJdkHome 'C:\Program Files\Java\jdk1.8.0_202'."
    exit 1
}

$releaseFile = Join-Path $javaHome "release"
if (Test-Path $releaseFile) {
    $releaseText = Get-Content -Raw -Path $releaseFile
    if ($releaseText -notmatch 'JAVA_VERSION="1\.8\.0_202"') {
        Write-Warning "Bundled JDK is not 1.8.0_202: $javaHome"
    }
}

Copy-Item -LiteralPath $clientJar -Destination (Join-Path $distDir "client-0.1.0-SNAPSHOT.jar")

if (-not $NoBundledJdk) {
    $runtimeSource = Join-Path $javaHome "jre"
    if (-not (Test-Path (Join-Path $runtimeSource "bin\java.exe"))) {
        $runtimeSource = $javaHome
    }
    Write-Host "Bundling Java runtime:"
    Write-Host $runtimeSource
    & robocopy $runtimeSource $runtimeDir /MIR /NFL /NDL /NJH /NJS /NP
    $robocopyExitCode = $LASTEXITCODE
    if ($robocopyExitCode -gt 7) {
        Write-Error "Failed to bundle Java runtime. Robocopy exit code: $robocopyExitCode"
        exit $robocopyExitCode
    }
    $global:LASTEXITCODE = 0
    Remove-JavaFxRuntimeFiles -RuntimeRoot $runtimeDir
}

if ($NoBundledJdk) {
    $runCmd = @(
        '@echo off',
        'setlocal',
        'set "APP_DIR=%~dp0"',
        'java -Dfile.encoding=UTF-8 -jar "%APP_DIR%client-0.1.0-SNAPSHOT.jar" %*'
    )
} else {
    $runCmd = @(
        '@echo off',
        'setlocal',
        'set "APP_DIR=%~dp0"',
        'set "JAVA_EXE=%APP_DIR%runtime\bin\java.exe"',
        'if not exist "%JAVA_EXE%" (',
        '  echo Missing bundled Java runtime: %JAVA_EXE%',
        '  echo Please rebuild the portable package with scripts\package-client.ps1.',
        '  pause',
        '  exit /b 1',
        ')',
        'set "PATH=%APP_DIR%runtime\bin;%PATH%"',
        '"%JAVA_EXE%" -Dfile.encoding=UTF-8 -jar "%APP_DIR%client-0.1.0-SNAPSHOT.jar" %*'
    )
}
Set-Content -Encoding ASCII -Path (Join-Path $distDir "run-client.cmd") -Value $runCmd

$runAdminCmd = @(
    '@echo off',
    'setlocal',
    "powershell.exe -NoProfile -ExecutionPolicy Bypass -Command `"Start-Process '%~dp0run-client.cmd' -Verb RunAs`""
)
Set-Content -Encoding ASCII -Path (Join-Path $distDir "run-client-admin.cmd") -Value $runAdminCmd

$zipPath = Join-Path $ProjectRoot "client\target\client-portable.zip"
if (Test-Path $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
Compress-Archive -Path (Join-Path $distDir "*") -DestinationPath $zipPath

Write-Host "Client package:"
Write-Host $clientJar
Write-Host "Portable client:"
Write-Host $distDir
if (-not $NoBundledJdk) {
    Write-Host "Bundled Java runtime:"
    Write-Host $runtimeDir
}
Write-Host "Portable zip:"
Write-Host $zipPath
