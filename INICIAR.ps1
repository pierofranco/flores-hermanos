param([int]$PuertoWeb=8080,[int]$PuertoPostgres=55432,[switch]$SinNavegador)
$ErrorActionPreference='Stop'
$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
Set-Location -LiteralPath $projectRoot
$appJar = Join-Path $projectRoot 'flores-hermanos.jar'
if (!(Test-Path -LiteralPath $appJar)) { throw 'Falta flores-hermanos.jar. Extraiga el paquete completo antes de iniciar.' }
if (!(Get-Command java -ErrorAction SilentlyContinue)) { throw 'Instale Java 17 o superior y vuelva a abrir este iniciador. Consulte LEEME.txt.' }
if ($PuertoWeb -lt 1024 -or $PuertoWeb -gt 65535 -or $PuertoPostgres -lt 1024 -or $PuertoPostgres -gt 65535) { throw 'Puertos no validos.' }
$pgBin = $env:FLORES_PG_BIN
if (!$pgBin) {
  $pgCommand = Get-Command pg_ctl -ErrorAction SilentlyContinue
  if ($pgCommand) { $pgBin = Split-Path $pgCommand.Source }
}
if (!$pgBin) {
  foreach ($version in @('18','17','16','15')) {
    $candidate = Join-Path $env:ProgramFiles "PostgreSQL\$version\bin"
    if (Test-Path -LiteralPath (Join-Path $candidate 'pg_ctl.exe')) { $pgBin = $candidate; break }
  }
}
if (!$pgBin -or !(Test-Path -LiteralPath (Join-Path $pgBin 'initdb.exe'))) { throw 'Instale PostgreSQL 15 o superior. O indique su carpeta bin en FLORES_PG_BIN. El sistema usa solamente PostgreSQL.' }
$dataPath = Join-Path $projectRoot 'datos-postgresql'
$configPath = Join-Path $projectRoot 'config-local.json'
$logPath = Join-Path $projectRoot 'logs'
New-Item -ItemType Directory -Path $logPath -Force | Out-Null
if (Test-Path -LiteralPath $configPath) {
  $config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
  $PuertoPostgres = [int]$config.port
  if ($config.host -ne '127.0.0.1' -or $config.database -ne 'flores_hermanos' -or $config.user -ne 'flores' -or $PuertoPostgres -lt 1024 -or $PuertoPostgres -gt 65535) { throw 'Revise config-local.json. Este iniciador administra solo el cluster local de este proyecto.' }
} else {
  if (Test-Path -LiteralPath (Join-Path $dataPath 'PG_VERSION')) { throw 'Existe un cluster sin config-local.json. Conserve los datos y siga el manual; no se sobrescribira.' }
  $randomBytes = New-Object byte[] 32
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($randomBytes)
  $config = [pscustomobject]@{host='127.0.0.1';port=$PuertoPostgres;database='flores_hermanos';user='flores';password=[Convert]::ToBase64String($randomBytes)}
  $pwFile = Join-Path $projectRoot 'work-init-password.tmp'
  try {
    [IO.File]::WriteAllText($pwFile,$config.password,[Text.UTF8Encoding]::new($false))
    Write-Host 'Preparando PostgreSQL para este proyecto. No se modificara su servicio existente.'
    & (Join-Path $pgBin 'initdb.exe') -D $dataPath -U flores --auth-host=scram-sha-256 --auth-local=scram-sha-256 --pwfile=$pwFile -E UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo inicializar PostgreSQL. Revise los permisos de la carpeta.' }
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
  } finally { if (Test-Path -LiteralPath $pwFile) { Remove-Item -LiteralPath $pwFile } }
}
$env:PGPASSWORD = $config.password
$env:FLORES_DB_URL = "jdbc:postgresql://127.0.0.1:$PuertoPostgres/flores_hermanos"
$env:FLORES_DB_USER = 'flores'
$env:FLORES_DB_PASSWORD = $config.password
$env:FLORES_PG_BIN = $pgBin
& (Join-Path $pgBin 'pg_ctl.exe') -D $dataPath status *> $null
$startedHere = $LASTEXITCODE -ne 0
$javaProcess = $null
try {
  if ($startedHere) {
    & (Join-Path $pgBin 'pg_ctl.exe') -D $dataPath -l (Join-Path $logPath 'postgresql.log') -o "-h 127.0.0.1 -p $PuertoPostgres -c timezone=America/Lima" -w start
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar PostgreSQL. Verifique que el puerto este libre y la version coincida con el cluster.' }
  }
  $exists = & (Join-Path $pgBin 'psql.exe') -h 127.0.0.1 -p $PuertoPostgres -U flores -d postgres -At -c "SELECT 1 FROM pg_database WHERE datname='flores_hermanos'"
  if ($LASTEXITCODE -ne 0) { throw 'No se pudo conectar al PostgreSQL local.' }
  if ($exists -ne '1') {
    & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p $PuertoPostgres -U flores flores_hermanos
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base de datos.' }
  }
  $javaProcess = Start-Process -FilePath (Get-Command java).Source -ArgumentList @('-jar',"`"$appJar`"","--server.port=$PuertoWeb") -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logPath 'aplicacion.log') -RedirectStandardError (Join-Path $logPath 'errores.log')
  $ready = $false
  for ($attempt=0;$attempt -lt 45;$attempt++) {
    if ($javaProcess.HasExited) { throw 'Java no pudo iniciar. Revise logs\aplicacion.log y logs\errores.log.' }
    try { $response = Invoke-WebRequest -Uri "http://127.0.0.1:$PuertoWeb/api/csrf" -UseBasicParsing -TimeoutSec 2; if ($response.StatusCode -eq 200) { $ready=$true; break } } catch {}
    Start-Sleep -Seconds 1
  }
  if (!$ready) { throw 'La aplicacion no respondio a tiempo. Revise los logs.' }
  Write-Host "Sistema listo en http://127.0.0.1:$PuertoWeb"
  Write-Host 'Usuarios demo: admin, vendedor, controlador, jefe y viajero'
  Write-Host 'Clave inicial: Flores2026!'
  Write-Host 'Mantenga esta ventana abierta. Ctrl+C detiene el sistema de forma controlada.'
  if (!$SinNavegador) { Start-Process "http://127.0.0.1:$PuertoWeb" }
  Wait-Process -Id $javaProcess.Id
} finally {
  if ($javaProcess -and !$javaProcess.HasExited) { Stop-Process -Id $javaProcess.Id -ErrorAction SilentlyContinue }
  if ($startedHere) { & (Join-Path $pgBin 'pg_ctl.exe') -D $dataPath -m fast -w stop }
  Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
