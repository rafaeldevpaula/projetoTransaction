# ========================================================
# Script de Execucao do Benchmark Paralelo
# Grupo: Gabriel, Rafael e Renan
# ========================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "Benchmark de Transacoes - JCRPGPD"

$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = Get-Location }
Set-Location $projectRoot

Clear-Host
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  LOCALIZANDO COMPILADOR E EXECUTAVEL JAVA..." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$javac = $null
$java = $null

# 1. Procura no PATH
if (Get-Command javac.exe -ErrorAction SilentlyContinue) {
    $javac = (Get-Command javac.exe).Source
    if (Get-Command java.exe -ErrorAction SilentlyContinue) {
        $java = (Get-Command java.exe).Source
    }
}

# 2. Procura em JAVA_HOME
if (-not $javac -and $env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    $javac = "$env:JAVA_HOME\bin\javac.exe"
    $java = "$env:JAVA_HOME\bin\java.exe"
}

# 3. Procura em pastas de extensoes do VS Code e Antigravity IDE
if (-not $javac) {
    $searchRoots = @(
        "$env:USERPROFILE\.antigravity-ide\extensions",
        "$env:USERPROFILE\.vscode\extensions",
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft"
    )

    foreach ($root in $searchRoots) {
        if (Test-Path $root) {
            $found = Get-ChildItem -Path $root -Filter "javac.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) {
                $javac = $found.FullName
                $java = Join-Path $found.Directory "java.exe"
                break
            }
        }
    }
}

if (-not $javac -or -not (Test-Path $javac)) {
    Write-Host "[ERRO] Nao foi possivel encontrar o compilador javac.exe!" -ForegroundColor Red
    Write-Host "Instale um JDK (versao 21+) ou instale a extensao de Java do VS Code." -ForegroundColor Yellow
    Read-Host "Pressione Enter para sair..."
    exit 1
}

Write-Host "[OK] Compilador : $javac" -ForegroundColor Green
Write-Host "[OK] Executavel : $java" -ForegroundColor Green
Write-Host ""

# ========================================================
# COMPILACAO
# ========================================================
Write-Host "Compilando codigo-fonte para a pasta 'out'..." -ForegroundColor Cyan
if (-not (Test-Path "out")) { New-Item -ItemType Directory -Path "out" | Out-Null }

$sources = Get-ChildItem -Recurse -Filter *.java src\main\java
& $javac -encoding UTF-8 -d out $sources.FullName

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha durante a compilacao dos fontes Java!" -ForegroundColor Red
    Read-Host "Pressione Enter para sair..."
    exit 1
}

Write-Host "[OK] Codigo compilado com sucesso!" -ForegroundColor Green
Start-Sleep -Milliseconds 800

# ========================================================
# MENU INTERATIVO
# ========================================================
while ($true) {
    Clear-Host
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host "  BENCHMARK PARALELO DE TRANSACOES FINANCEIRAS" -ForegroundColor Cyan
    Write-Host "  Grupo: Gabriel, Rafael e Renan" -ForegroundColor Gray
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Escolha o volume de transacoes para testar:" -ForegroundColor White
    Write-Host ""
    Write-Host "  [1] Teste Rapido    -    100.000 transacoes (Padrao)" -ForegroundColor Green
    Write-Host "  [2] Teste Medio     -  1.000.000 transacoes (1 Milhao)" -ForegroundColor Yellow
    Write-Host "  [3] Teste Pesado    -  5.000.000 transacoes (5 Milhoes)" -ForegroundColor Magenta
    Write-Host "  [4] Limite Maximo   - 10.000.000 transacoes (10 Milhoes)" -ForegroundColor Red
    Write-Host "  [5] Personalizado   - Escolher quantidade e threads manualmente" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  [0] Sair" -ForegroundColor DarkGray
    Write-Host ""

    $opcao = Read-Host "Digite a opcao desejada [1-5]"

    if ($opcao -eq "0") {
        Write-Host "Encerrando benchmark..." -ForegroundColor Gray
        break
    }

    $qtd = 100000
    $threads = "1,2,4,8"
    $memoria = "-Xmx2g"

    switch ($opcao) {
        "1" {
            $qtd = 100000
            $threads = "1,2,4,8"
            $memoria = "-Xmx2g"
        }
        "2" {
            $qtd = 1000000
            $threads = "1,2,4,8"
            $memoria = "-Xmx4g"
        }
        "3" {
            $qtd = 5000000
            $threads = "1,2,4,8"
            $memoria = "-Xmx6g"
        }
        "4" {
            $qtd = 10000000
            $threads = "1,2,4,8"
            $memoria = "-Xmx8g"
        }
        "5" {
            Write-Host ""
            $inputQtd = Read-Host "Quantidade de transacoes [padrao: 100000]"
            if ($inputQtd) { $qtd = [int]$inputQtd }

            $inputThreads = Read-Host "Threads a testar [padrao: 1,2,4,8]"
            if ($inputThreads) { $threads = $inputThreads }

            if ($qtd -ge 8000000) { $memoria = "-Xmx8g" }
            elseif ($qtd -ge 3000000) { $memoria = "-Xmx6g" }
            elseif ($qtd -ge 1000000) { $memoria = "-Xmx4g" }
            else { $memoria = "-Xmx2g" }
        }
        default {
            Write-Host "Opcao invalida! Tente novamente." -ForegroundColor Red
            Start-Sleep -Seconds 1
            continue
        }
    }

    Clear-Host
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host "  EXECUTANDO BENCHMARK" -ForegroundColor Cyan
    Write-Host "  Quantidade : $qtd" -ForegroundColor White
    Write-Host "  Threads    : $threads" -ForegroundColor White
    Write-Host "  Memoria    : $memoria" -ForegroundColor White
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host ""

    # Execucao do Java
    & $java $memoria -cp out app.Main --quantidade=$qtd --threads=$threads

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "  EXECUCAO CONCLUIDA COM SUCESSO!" -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host ""

    $abrirHtml = Read-Host "Deseja abrir o relatorio visual (resultados.html) no navegador? (S/N) [S]"
    if ($abrirHtml -ne "n" -and $abrirHtml -ne "N") {
        if (Test-Path "resultados.html") {
            Start-Process "resultados.html"
        }
    }

    Write-Host ""
    $novaRodada = Read-Host "Deseja testar outro volume de transacoes? (S/N) [S]"
    if ($novaRodada -eq "n" -or $novaRodada -eq "N") {
        Write-Host "Finalizando..." -ForegroundColor Gray
        break
    }
}
