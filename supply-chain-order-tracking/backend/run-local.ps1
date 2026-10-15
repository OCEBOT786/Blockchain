$config = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $config)) {
    throw 'Copy .env.example to .env, fill the server-side values, then run this script.'
}

foreach ($line in Get-Content -LiteralPath $config) {
    if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), 'Process')
    }
}

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
