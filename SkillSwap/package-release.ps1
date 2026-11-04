$ErrorActionPreference = 'Stop'
$source = (Get-Location).Path
$stage = Join-Path $source '.package-stage'
$output = Join-Path (Split-Path (Split-Path $source -Parent) -Parent) 'outputs\SkillSwap_corrigido_completo_20261103_v2.zip'

if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null

$excluded = '\\(node_modules|dist|target|\.git|\.m2-local|\.maven-user|\.tmp)(\\|$)|backend\\skill-swap-0\.0\.1-SNAPSHOT\.jar$|(^|[\\/])\.env($|\.)'
$files = Get-ChildItem -LiteralPath $source -Recurse -File |
    Where-Object { $_.FullName -notmatch $excluded -and $_.FullName -notmatch '\\package-release\.ps1$' }

foreach ($file in $files) {
    $relative = $file.FullName.Substring($source.Length + 1)
    $destination = Join-Path $stage $relative
    $destinationDir = Split-Path $destination -Parent
    New-Item -ItemType Directory -Force -Path $destinationDir | Out-Null
    Copy-Item -LiteralPath $file.FullName -Destination $destination -Force
}

if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output -Force }
Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $output -CompressionLevel Optimal
Remove-Item -LiteralPath $stage -Recurse -Force
Get-Item -LiteralPath $output | Select-Object FullName, Length
