param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
$isAdmin = $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    $script = $MyInvocation.MyCommand.Path
    $args = "-NoProfile -ExecutionPolicy Bypass -File `"$script`" -ProjectRoot `"$ProjectRoot`""
    Start-Process powershell.exe -Verb RunAs -ArgumentList $args
    exit
}

Set-Location $ProjectRoot
$clientJar = Join-Path $ProjectRoot "client\target\client-0.1.0-SNAPSHOT.jar"
$distRunner = Join-Path $ProjectRoot "client\target\dist\run-client.cmd"
if (-not (Test-Path $clientJar)) {
    mvn -pl client -am clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

if (Test-Path $distRunner) {
    & $distRunner
} else {
    java -jar $clientJar
}
