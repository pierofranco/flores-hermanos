$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
if (!(Test-Path -LiteralPath 'config-local.json')) { throw 'Inicie primero el sistema con INICIAR.bat. Mantengalo abierto mientras ejecuta las pruebas.' }
if (!(Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Para ejecutar las pruebas desde el codigo necesita Maven y un JDK 17 o superior.' }
$cfg=Get-Content -LiteralPath 'config-local.json' -Raw | ConvertFrom-Json
$version=(Get-Content -LiteralPath 'datos-postgresql/PG_VERSION').Trim()
$pgBin=$env:FLORES_PG_BIN
if (!$pgBin) { $pgBin=Join-Path $env:ProgramFiles "PostgreSQL\$version\bin" }
$testDb='flores_pruebas_'+(Get-Date -Format 'yyyyMMddHHmmss')
$env:PGPASSWORD=$cfg.password
$env:FLORES_TEST_DB_URL="jdbc:postgresql://127.0.0.1:$($cfg.port)/$testDb"
$env:FLORES_TEST_DB_USER=$cfg.user
$env:FLORES_TEST_DB_PASSWORD=$cfg.password
try {
  & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p $cfg.port -U $cfg.user $testDb
  if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base PostgreSQL aislada de pruebas.' }
  Push-Location -LiteralPath 'codigo'
  try { & mvn test; if ($LASTEXITCODE -ne 0) { throw 'Hay pruebas fallidas. Revise codigo/target/surefire-reports.' } } finally { Pop-Location }
  Write-Host "Pruebas terminadas. Se conserva la base aislada $testDb para inspeccion. La base flores_hermanos no se ha usado para las pruebas."
} finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
