param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [switch]$SkipClean
)

Set-Location $ProjectRoot
$mavenArgs = @("-pl", "server", "-am")
if (-not $SkipClean) {
    $mavenArgs += "clean"
}
$mavenArgs += @("package", "-DskipTests")
& mvn @mavenArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$serverJar = Join-Path $ProjectRoot "server\target\server-0.1.0-SNAPSHOT.jar"
Write-Host "Server package:"
Write-Host $serverJar
