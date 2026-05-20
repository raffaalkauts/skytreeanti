$sourceDir = "d:\projek jadi\vscodetest\skytree antig\src\main\java"
Get-ChildItem -Path $sourceDir -Filter *.java -Recurse | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file
    
    # 1. Replace BankUtil.formatBTC(...) + " BTC" with BankUtil.formatCurrency(...)
    $content = $content -replace 'BankUtil\.formatBTC\(([^)]+)\)\s*\+\s*" BTC"', 'BankUtil.formatCurrency($1)'
    
    # 2. Replace any remaining BankUtil.formatBTC with BankUtil.formatCurrency (in case it didn't have + " BTC")
    $content = $content -replace 'BankUtil\.formatBTC', 'BankUtil.formatCurrency'
    
    # 3. Replace loose " BTC" literals with " USDT"
    $content = $content -replace '" BTC"', '" USDT"'
    $content = $content -replace '" BTC"', '" USDT"'
    
    $content | Set-Content $file
}
Write-Host "Finished banking and global BTC replacement."
