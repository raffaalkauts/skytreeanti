$sourceDir = "d:\projek jadi\vscodetest\skytree antig\src\main\java"
Get-ChildItem -Path $sourceDir -Filter *.java -Recurse | ForEach-Object {
    $file = $_.FullName
    Write-Host "Processing $file"
    $content = Get-Content $file
    $newContent = $content -replace 'NumberUtil\.formatBTC', 'NumberUtil.formatCurrency'
    $newContent | Set-Content $file
}
Write-Host "Finished search and replace."
