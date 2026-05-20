$file = "d:\projek jadi\vscodetest\skytree antig\src\main\resources\shop.yml"
$content = Get-Content $file -Raw

# Define replacements with appropriate sell prices (typically 10-20% of buy price)
$replacements = @{
    'suspicious_sand: { price: 50, sell: 0 }'          = 'suspicious_sand: { price: 50, sell: 10 }'
    'suspicious_gravel: { price: 50, sell: 0 }'        = 'suspicious_gravel: { price: 50, sell: 10 }'
    'experience_bottle: { price: 50, sell: 0 }'        = 'experience_bottle: { price: 50, sell: 10 }'
    'trident: { price: 100000, sell: 0 }'              = 'trident: { price: 100000, sell: 20000 }'
    'elytra: { price: 50000, sell: 0 }'                = 'elytra: { price: 50000, sell: 10000 }'
    'enchanted_golden_apple: { price: 5000, sell: 0 }' = 'enchanted_golden_apple: { price: 5000, sell: 1000 }'
    'sell_wand: { price: 100000, sell: 0 }'            = 'sell_wand: { price: 100000, sell: 0 }' # Keep 0 for special items
    'spawner_warden: { price: 5000000, sell: 0 }'      = 'spawner_warden: { price: 5000000, sell: 0 }' # Keep 0 for boss spawners
    'ORACLE_CHEST_BASIC: { price: 10000, sell: 0 }'    = 'ORACLE_CHEST_BASIC: { price: 10000, sell: 2000 }'
    'ORACLE_CHEST_PREMIUM: { price: 50000, sell: 0 }'  = 'ORACLE_CHEST_PREMIUM: { price: 50000, sell: 10000 }'
    'ORACLE_CHEST_DIVINE: { price: 250000, sell: 0 }'  = 'ORACLE_CHEST_DIVINE: { price: 250000, sell: 50000 }'
    'pebble_andesite: { price: 1, sell: 0 }'           = 'pebble_andesite: { price: 1, sell: 0 }' # Keep 0 for very cheap items
    'pebble_diorite: { price: 1, sell: 0 }'            = 'pebble_diorite: { price: 1, sell: 0 }'
    'pebble_basalt: { price: 1, sell: 0 }'             = 'pebble_basalt: { price: 1, sell: 0 }'
    'pebble_blackstone: { price: 1, sell: 0 }'         = 'pebble_blackstone: { price: 1, sell: 0 }'
    'piece_iron: { price: 2, sell: 0 }'                = 'piece_iron: { price: 2, sell: 0 }' # Keep 0 for ore pieces (intermediate items)
    'piece_gold: { price: 3, sell: 0 }'                = 'piece_gold: { price: 3, sell: 0 }'
    'piece_copper: { price: 1, sell: 0 }'              = 'piece_copper: { price: 1, sell: 0 }'
    'piece_coal: { price: 1, sell: 0 }'                = 'piece_coal: { price: 1, sell: 0 }'
    'piece_tin: { price: 1, sell: 0 }'                 = 'piece_tin: { price: 1, sell: 0 }'
    'piece_aluminum: { price: 1, sell: 0 }'            = 'piece_aluminum: { price: 1, sell: 0 }'
    'piece_silver: { price: 1, sell: 0 }'              = 'piece_silver: { price: 1, sell: 0 }'
    'piece_lead: { price: 1, sell: 0 }'                = 'piece_lead: { price: 1, sell: 0 }'
    'piece_nickel: { price: 1, sell: 0 }'              = 'piece_nickel: { price: 1, sell: 0 }'
    'dimensional_key: { price: 1000000, sell: 0 }'     = 'dimensional_key: { price: 1000000, sell: 200000 }'
    'water_bottle: { price: 10, sell: 0 }'             = 'water_bottle: { price: 10, sell: 2 }'
}

foreach ($key in $replacements.Keys) {
    $content = $content -replace [regex]::Escape($key), $replacements[$key]
}

Set-Content -Path $file -Value $content -NoNewline
Write-Output "Updated shop.yml - Fixed all sell: 0 items"
