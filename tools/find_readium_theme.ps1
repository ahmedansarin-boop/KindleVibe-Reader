Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1\org.readium.kotlin-toolkit'
if (!(Test-Path -LiteralPath $root)) {
    Write-Error "Root not found: $root"
    exit 1
}

$jars = Get-ChildItem -LiteralPath $root -Recurse -Filter '*.jar' |
    Where-Object { $_.Name -match 'navigator' }

foreach ($j in $jars) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($j.FullName)
    try {
        $entries = $zip.Entries | Where-Object { $_.FullName -match 'Theme' }
        if ($entries -and $entries.Count -gt 0) {
            Write-Output "JAR: $($j.FullName)"
            foreach ($e in $entries) {
                Write-Output $e.FullName
            }
            Write-Output ""
        }
    } finally {
        $zip.Dispose()
    }
}
