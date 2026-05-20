$version = "1.21.1"
$url = "https://api.papermc.io/v2/projects/paper/versions/$version"
$output = "libs\paper-server.jar"

try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    
    Write-Host "Fetching latest build info for Paper $version..."
    $response = Invoke-RestMethod -Uri $url
    $latestBuild = $response.builds[-1]
    
    $downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/$version/builds/$latestBuild/downloads/paper-$version-$latestBuild.jar"
    
    Write-Host "Detected latest build: $latestBuild"
    Write-Host "Downloading from $downloadUrl..."
    
    Invoke-WebRequest -Uri $downloadUrl -OutFile $output
    
    Write-Host "Download complete: $output"

    # Download Annotations
    Write-Host "Downloading JetBrains Annotations..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/jetbrains/annotations/24.0.0/annotations-24.0.0.jar" -OutFile "libs\annotations.jar"

    # Download Kyori Adventure (Compilation only)
    Write-Host "Downloading Kyori Adventure dependencies for build..."
    $advVersion = "4.17.0"
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/kyori/adventure-api/$advVersion/adventure-api-$advVersion.jar" -OutFile "libs\adventure-api.jar"
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/kyori/adventure-text-minimessage/$advVersion/adventure-text-minimessage-$advVersion.jar" -OutFile "libs\adventure-minimessage.jar"
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/kyori/adventure-key/$advVersion/adventure-key-$advVersion.jar" -OutFile "libs\adventure-key.jar"
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-legacy/$advVersion/adventure-text-serializer-legacy-$advVersion.jar" -OutFile "libs\adventure-serializer-legacy.jar"
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-plain/$advVersion/adventure-text-serializer-plain-$advVersion.jar" -OutFile "libs\adventure-serializer-plain.jar"
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar" -OutFile "libs\examination-api.jar"
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar" -OutFile "libs\examination-string.jar"

    Write-Host "All build libraries downloaded successfully."
}
catch {
    Write-Error "Error: $_"
    exit 1
}
