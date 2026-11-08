$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$testDir = Join-Path $project 'target\verificacion'
New-Item -ItemType Directory -Force $testDir | Out-Null
$jdk = $env:JAVA_HOME
if (!$jdk -or !(Test-Path (Join-Path $jdk 'bin\javac.exe'))) {
    $jdk = (Get-ChildItem 'C:\Program Files\Java' -Directory | Where-Object Name -Like 'jdk-*' | Sort-Object Name -Descending | Select-Object -First 1).FullName
}
$java = Join-Path $jdk 'bin\java.exe'
$javac = Join-Path $jdk 'bin\javac.exe'
$repo = Join-Path $env:USERPROFILE '.m2\repository'
$fx = @('base','graphics','controls','fxml') | ForEach-Object { Join-Path $repo "org\openjfx\javafx-$_\23\javafx-$_-23-win.jar" }
$sqlite = Join-Path $repo 'org\xerial\sqlite-jdbc\3.45.1.0\sqlite-jdbc-3.45.1.0.jar'
$slf = Join-Path $repo 'org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar'
foreach ($file in ($fx + @($sqlite,$slf))) {if (!(Test-Path -LiteralPath $file)) {throw "Falta dependencia: $file. Compila el proyecto con Maven primero."}}
$classes = Join-Path $testDir 'classes'
$mp = ($fx + @($sqlite)) -join ';'
$sources = (Get-ChildItem (Join-Path $project 'src\main\java') -Recurse -Filter '*.java').FullName
& $javac -encoding UTF-8 --module-path $mp -d $classes $sources
if ($LASTEXITCODE -ne 0) {throw 'Error de compilacion'}
$resources = Join-Path $project 'src\main\resources'
$cp = @($classes,$resources,$sqlite,$slf) -join ';'
& $javac -encoding UTF-8 --module-path ($fx -join ';') --add-modules javafx.controls,javafx.fxml -cp $cp -d $testDir (Join-Path $PSScriptRoot 'IntegrationCheck.java') (Join-Path $PSScriptRoot 'FxmlCheck.java')
if ($LASTEXITCODE -ne 0) {throw 'Error al compilar pruebas'}
$testDb = Join-Path $testDir 'pruebas.db'
Copy-Item -LiteralPath (Join-Path $project 'panaderia.db') -Destination $testDb -Force
& $java -cp "$testDir;$cp" IntegrationCheck $testDb
if ($LASTEXITCODE -ne 0) {throw 'Fallo de integracion'}
& $java --module-path ($fx -join ';') --add-modules javafx.controls,javafx.fxml -cp "$testDir;$cp" FxmlCheck $testDb $resources
if ($LASTEXITCODE -ne 0) {throw 'Fallo de pantallas'}
Write-Host 'VERIFICACION COMPLETA: base de datos y pantallas correctas. La base original no se modifico.'
