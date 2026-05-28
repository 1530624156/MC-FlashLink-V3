param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

Set-Location $ProjectRoot
mvn -pl server -am spring-boot:run
