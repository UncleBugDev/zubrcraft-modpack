# ZubrCraft Fast Auto-Sync for Prism Launcher
$ErrorActionPreference = 'SilentlyContinue'
$ProgressPreference = 'SilentlyContinue'

$syncUrl = "https://sync.zubrcraft.fun"
$manifestUrl = "$syncUrl/manifest.json"

$mcDir = if ($PSScriptRoot) { $PSScriptRoot } else { Get-Location }
$modsDir = Join-Path $mcDir "mods"
$localServers = Join-Path $mcDir "servers.dat"

if (-not (Test-Path $modsDir)) {
    New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
}

try {
    # 3-second timeout for fast startup check
    $webClient = New-Object System.Net.WebClient
    $webClient.Headers.Add("User-Agent", "ZubrCraft-Sync/1.0")
    $jsonContent = $webClient.DownloadString($manifestUrl)
    $manifest = $jsonContent | ConvertFrom-Json
} catch {
    # If offline or CDN unreachable, immediately let Minecraft launch
    exit 0
}

if (-not $manifest -or -not $manifest.files) {
    exit 0
}

# 1. Auto-update servers.dat to include Reserve Server if not present
try {
    $serversUrl = "$syncUrl/servers.dat"
    $tmpServers = "$localServers.tmp"
    $sClient = New-Object System.Net.WebClient
    $sClient.Headers.Add("User-Agent", "ZubrCraft-Sync/1.0")
    $sClient.DownloadFile($serversUrl, $tmpServers)
    if (Test-Path $tmpServers) {
        $shouldUpdate = $true
        if (Test-Path $localServers) {
            $raw = [System.IO.File]::ReadAllBytes($localServers)
            $txt = [System.Text.Encoding]::UTF8.GetString($raw)
            if ($txt.Contains("vivo-whomever.tun.ply.gg")) {
                $shouldUpdate = $false
            }
        }
        if ($shouldUpdate) {
            Move-Item -Path $tmpServers -Destination $localServers -Force
        } else {
            Remove-Item -Path $tmpServers -Force -ErrorAction SilentlyContinue
        }
    }
} catch {}

# 2. Sync mods
$expectedFiles = @{}
foreach ($item in $manifest.files) {
    $fileName = $item.name
    $expectedFiles[$fileName] = $true
    $targetPath = Join-Path $modsDir $fileName
    
    $needsDownload = $false
    if (-not (Test-Path $targetPath)) {
        $needsDownload = $true
    } else {
        $fInfo = Get-Item $targetPath
        if ($item.size -gt 0 -and $fInfo.Length -ne $item.size) {
            $needsDownload = $true
        }
    }

    if ($needsDownload) {
        $fileUrl = "$syncUrl/mods/$fileName"
        $tmpPath = "$targetPath.tmp"
        try {
            $dlClient = New-Object System.Net.WebClient
            $dlClient.Headers.Add("User-Agent", "ZubrCraft-Sync/1.0")
            $dlClient.DownloadFile($fileUrl, $tmpPath)
            if (Test-Path $tmpPath) {
                Move-Item -Path $tmpPath -Destination $targetPath -Force
            }
        } catch {
            if (Test-Path $tmpPath) { Remove-Item -Path $tmpPath -Force -ErrorAction SilentlyContinue }
        }
    }
}

# Remove obsolete mods no longer on the server
$currentJars = Get-ChildItem -Path $modsDir -Filter "*.jar" -ErrorAction SilentlyContinue
foreach ($f in $currentJars) {
    if (-not $expectedFiles.ContainsKey($f.Name)) {
        Remove-Item -Path $f.FullName -Force -ErrorAction SilentlyContinue
    }
}

exit 0
