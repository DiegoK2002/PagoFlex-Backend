$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$modulos = @(
    'usuarios-service',
    'empresas-service',
    'compromisos-service',
    'pagos-service',
    'reportes-service',
    'archivos-service',
    'api-gateway',
    'eureka-server'
)

$resultados = @()
$totalCubiertas = [long]0
$totalOmitidas = [long]0
$totalPruebas = 0
$totalErrores = 0

foreach ($modulo in $modulos) {
    Write-Host "`n=== $modulo ===" -ForegroundColor Cyan
    $carpeta = Join-Path $raiz $modulo
    $maven = Join-Path $carpeta 'mvnw.cmd'
    if (-not (Test-Path $maven)) { throw "No se encontro Maven Wrapper en $modulo" }
    Push-Location $carpeta
    try {
        & $maven --batch-mode verify
        if ($LASTEXITCODE -ne 0) { throw "FALLARON las pruebas de $modulo (codigo $LASTEXITCODE)" }
    }
    finally { Pop-Location }

    $rutaXml = Join-Path $carpeta 'target\site\jacoco\jacoco.xml'
    if (-not (Test-Path $rutaXml)) { throw "No se genero el informe JaCoCo de $modulo" }
    [xml]$informe = Get-Content -Raw $rutaXml
    $contador = @($informe.report.counter | Where-Object { $_.type -eq 'INSTRUCTION' })[0]
    if ($null -eq $contador) { throw "Informe sin contador INSTRUCTION: $modulo" }
    $cubiertas = [long]$contador.covered
    $omitidas = [long]$contador.missed
    $totalCubiertas += $cubiertas
    $totalOmitidas += $omitidas
    $denominador = $cubiertas + $omitidas
    $porcentaje = if ($denominador -eq 0) { 0 } else { [math]::Round(100 * $cubiertas / $denominador, 2) }

    $pruebas = 0
    $errores = 0
    $surefire = Join-Path $carpeta 'target\surefire-reports'
    foreach ($xmlTest in @(Get-ChildItem $surefire -Filter 'TEST-*.xml' -File -ErrorAction SilentlyContinue)) {
        [xml]$test = Get-Content -Raw $xmlTest.FullName
        $pruebas += [int]$test.testsuite.tests
        $errores += [int]$test.testsuite.failures + [int]$test.testsuite.errors
    }
    $totalPruebas += $pruebas
    $totalErrores += $errores
    $resultados += [pscustomobject]@{
        Microservicio = $modulo
        Pruebas = $pruebas
        Errores = $errores
        CoberturaInstrucciones = "$porcentaje%"
        Cubiertas = $cubiertas
        Omitidas = $omitidas
    }
}

$denominadorTotal = $totalCubiertas + $totalOmitidas
$coberturaTotal = if ($denominadorTotal -eq 0) { 0 } else { [math]::Round(100 * $totalCubiertas / $denominadorTotal, 2) }
Write-Host "`n=== RESULTADOS DEL BACKEND ===" -ForegroundColor Cyan
$resultados | Format-Table Microservicio, Pruebas, Errores, CoberturaInstrucciones -AutoSize
Write-Host "Pruebas ejecutadas: $totalPruebas"
Write-Host "Errores: $totalErrores"
Write-Host "Cobertura GLOBAL de instrucciones: $coberturaTotal%"

$salida = Join-Path $raiz 'Cobertura-Backend-Resultado.csv'
$resultados | Export-Csv $salida -Delimiter ';' -NoTypeInformation -Encoding UTF8
Write-Host "Detalle guardado en: $salida"
if ($totalErrores -gt 0) { exit 1 }
if ($coberturaTotal -lt 80) {
    Write-Host "La cobertura global NO alcanza el 80%." -ForegroundColor Yellow
    exit 2
}
Write-Host "Cobertura global igual o superior al 80%." -ForegroundColor Green
