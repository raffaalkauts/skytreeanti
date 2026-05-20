$sourceDir = "d:\projek jadi\vscodetest\skytree antig\src\main\java"
Get-ChildItem -Path $sourceDir -Filter *.java -Recurse | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file
    
    # 1. Fix double USDT suffixes (formatCurrency already includes USDT)
    $content = $content -replace 'BankUtil\.formatCurrency\(([^)]+)\)\s*\+\s*" USDT"', 'BankUtil.formatCurrency($1)'
    $content = $content -replace 'NumberUtil\.formatCurrency\(([^)]+)\)\s*\+\s*" USDT"', 'NumberUtil.formatCurrency($1)'
    
    # 2. Replace any word "BTC" with "USDT" (using word boundaries \b)
    $content = $content -replace '\bBTC\b', 'USDT'
    
    $content | Set-Content $file
}
Write-Host "Finished final USDT cleanup."
