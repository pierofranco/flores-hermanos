param([Parameter(Mandatory=$true)][string]$Archivo,[Parameter(Mandatory=$true)][string]$NuevaBase)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
if ($NuevaBase -notmatch '^flores_restaurada_[a-z0-9_]{1,40}$') { throw 'El nombre debe comenzar con flores_restaurada_ y continuar con letras minusculas, numeros o guion bajo.' }
if (!(Test-Path -LiteralPath $Archivo)) { throw 'No se encontro el respaldo indicado.' }
$cfg=Get-Content -LiteralPath 'config-local.json' -Raw | ConvertFrom-Json
$version=(Get-Content -LiteralPath 'datos-postgresql/PG_VERSION').Trim()
$pgBin=$env:FLORES_PG_BIN
if (!$pgBin) { $pgBin=Join-Path $env:ProgramFiles "PostgreSQL\$version\bin" }
$env:PGPASSWORD=$cfg.password
try {
  & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p $cfg.port -U $cfg.user $NuevaBase
  if ($LASTEXITCODE -ne 0) { throw 'No se creo la base nueva. Si ya existe, elija otro nombre. No se sobrescribe ninguna base.' }
  & (Join-Path $pgBin 'pg_restore.exe') -h 127.0.0.1 -p $cfg.port -U $cfg.user -d $NuevaBase --no-owner --exit-on-error $Archivo
  if ($LASTEXITCODE -ne 0) { throw 'La restauracion no termino. Revise la base nueva y el registro; la original permanece intacta.' }
  Write-Host "Respaldo restaurado en $NuevaBase. La base original no se modifico."
  Write-Host 'Revise los registros antes de configurar la aplicacion para usar esta copia.'
} finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
