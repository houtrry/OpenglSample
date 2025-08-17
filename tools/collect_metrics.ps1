# PowerShell: 自动采集 Android 性能指标并合并为 CSV
param(
    [string]$Package = "com.example.app",
    [int]$DurationSec = 60,
    [string]$OutputDir = "metrics_out"
)

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Error "adb 未安装或不可用。请先安装 Android Platform-Tools 并配置 PATH。"
    exit 1
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$framestatsFile = Join-Path $OutputDir "framestats_$ts.txt"
$topFile = Join-Path $OutputDir "top_$ts.txt"
$memFile = Join-Path $OutputDir "mem_$ts.txt"
$perfettoFile = Join-Path $OutputDir "gpu_$ts.pftrace"
$mergedCsv = Join-Path $OutputDir "merged_$ts.csv"

Write-Host "开始采集 $DurationSec 秒..." -ForegroundColor Cyan

# 1) gfxinfo framestats（采集后导出）
Start-Job -ScriptBlock {
    param($Package, $framestatsFile)
    Start-Sleep -Seconds 2
    adb shell cmd gfxinfo $Package framestats | Out-File -Encoding utf8 $framestatsFile
} -ArgumentList $Package, $framestatsFile | Out-Null

# 2) top 连续采样
Start-Job -ScriptBlock {
    param($Package, $topFile, $DurationSec)
    $end = (Get-Date).AddSeconds($DurationSec)
    while ((Get-Date) -lt $end) {
        adb shell top -b -n 1 | Select-String $Package | Add-Content -Encoding utf8 $topFile
        Start-Sleep -Seconds 1
    }
} -ArgumentList $Package, $topFile, $DurationSec | Out-Null

# 3) meminfo 周期采样（每 2s）
Start-Job -ScriptBlock {
    param($Package, $memFile, $DurationSec)
    $end = (Get-Date).AddSeconds($DurationSec)
    while ((Get-Date) -lt $end) {
        adb shell dumpsys meminfo $Package | Out-File -Append -Encoding utf8 $memFile
        Start-Sleep -Seconds 2
    }
} -ArgumentList $Package, $memFile, $DurationSec | Out-Null

# 4) perfetto（若设备支持）
Start-Job -ScriptBlock {
    param($perfettoFile, $DurationSec)
    try {
        adb shell perfetto -o /data/misc/perfetto-traces/gpu_tmp.pftrace -t ${DurationSec}s sched freq idle am wm gfx view 2>$null
        adb pull /data/misc/perfetto-traces/gpu_tmp.pftrace $perfettoFile | Out-Null
    } catch {}
} -ArgumentList $perfettoFile, $DurationSec | Out-Null

Start-Sleep -Seconds $DurationSec

Write-Host "采集已结束，开始从应用导出 metrics.csv（若已集成 PerfMetrics）..." -ForegroundColor Cyan
try {
    $localMetrics = Join-Path $OutputDir "metrics_$ts.csv"
    adb pull "/sdcard/Android/data/$Package/files/metrics.csv" $localMetrics | Out-Null
    if (-not (Test-Path $localMetrics)) {
        adb shell run-as $Package cat files/metrics.csv | Out-File -Encoding utf8 $localMetrics
    }
} catch {}

# 简单合并示例：仅拷贝应用侧 CSV 作为基准
if (Test-Path (Join-Path $OutputDir "metrics_$ts.csv")) {
    Copy-Item (Join-Path $OutputDir "metrics_$ts.csv") $mergedCsv -Force
    Write-Host "合并完成：$mergedCsv" -ForegroundColor Green
} else {
    Write-Host "未发现应用侧 metrics.csv，已保留原始文本输出。" -ForegroundColor Yellow
}

Write-Host "输出目录：$(Resolve-Path $OutputDir)" -ForegroundColor Cyan


