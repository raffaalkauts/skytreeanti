# How to Compile Skytree Plugin

## Option 1: Using Maven (Recommended)

### Install Maven First
If you don't have Maven installed:

**Windows:**
1. Download Maven from: https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH: `C:\Program Files\Apache\maven\bin`
4. Open new PowerShell and verify: `mvn -version`

**Then Build:**
```bash
cd "d:\projek jadi\vscodetest\skytree antig"
mvn clean package
```

Output JAR: `target/Skytree-v3.2.0.jar`

## Option 2: Using IDE (IntelliJ IDEA)

1. Open IntelliJ IDEA
2. File → Open → Select `d:\projek jadi\vscodetest\skytree antig`
3. Wait for Maven to import dependencies
4. View → Tool Windows → Maven
5. Click on `skytree → Lifecycle → package`
6. Output JAR in `target/` folder

## Option 3: Using VS Code with Java Extension

1. Install "Extension Pack for Java" in VS Code
2. Open folder: `d:\projek jadi\vscodetest\skytree antig`
3. Press `Ctrl+Shift+P`
4. Type "Java: Export Jar"
5. Follow prompts

## Verification

After building, you should see:
```
target/Skytree-v3.2.0.jar
```

Size should be approximately 50-100 KB.

## Testing on Server

1. Copy `target/Skytree-v3.2.0.jar` to your PaperMC server's `plugins/` folder
2. Start/restart the server
3. Look for in console:
   ```
   [Skytree] Initializing Skytree...
   [Skytree] Creating void world: skytree_world
   [Skytree] Registered 50 custom items!
   [Skytree] Skytree enabled successfully with 80+ features!
   ```
4. Join server and type `/is create`

## Troubleshooting

**"mvn not found"**
→ Maven needs to be installed (see Option 1 above)

**IDE can't resolve dependencies**
→ Right-click `pom.xml` → Maven → Reload Project

**Build errors**
→ Ensure Java 21 is installed: `java -version`
→ Should show version 21.x.x
