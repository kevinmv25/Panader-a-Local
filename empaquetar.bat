@echo off
setlocal
cd /d "%~dp0"

rem ============================================================
rem  Genera la aplicacion de escritorio "El Lirio Rojo"
rem  Uso: doble clic en este archivo (debe estar junto a pom.xml)
rem  Cierra la aplicacion antes de ejecutarlo.
rem ============================================================

set "JAVA_HOME=C:\Users\Sinuhe\tools\openjdk-23+37_windows-x64_bin\jdk-23"
set "MVN=C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
set "JAR=Panaderia-Local-1.0-SNAPSHOT-fat.jar"
set "APP=El Lirio Rojo"
set "LOGO_PNG=src\main\resources\com\mycompany\panaderia\local\iconos\logo_el_lirio_rojo_hd.png"
set "ICONO=icono\logo.ico"

if not exist pom.xml (
    echo ERROR: este archivo debe estar en la carpeta del proyecto, junto a pom.xml
    pause
    exit /b 1
)

echo.
echo Preparando el icono de la aplicacion...
set "OPCION_ICONO="
if not exist "%ICONO%" (
    if exist "%LOGO_PNG%" (
        if exist herramientas\ConvertirIco.java (
            "%JAVA_HOME%\bin\java.exe" herramientas\ConvertirIco.java "%LOGO_PNG%" "%ICONO%"
        )
    )
)
if exist "%ICONO%" (
    set "OPCION_ICONO=--icon icono\logo.ico"
    echo Icono: %ICONO%
) else (
    echo AVISO: no hay %ICONO%. Se usara el icono por defecto de Java.
)

echo.
echo [1/5] Compilando el proyecto (puede tardar un poco)...
call "%MVN%" --no-transfer-progress clean package
if errorlevel 1 (
    echo.
    echo ERROR al compilar. Revisa los mensajes de arriba.
    pause
    exit /b 1
)

if not exist "target\%JAR%" (
    echo.
    echo ERROR: no se genero target\%JAR%
    echo Revisa que el pom.xml tenga el plugin maven-shade-plugin.
    pause
    exit /b 1
)

echo.
echo [2/5] Comprobando que la plantilla de base de datos quedo dentro del programa...
"%JAVA_HOME%\bin\jar.exe" tf "target\%JAR%" | findstr /c:"plantilla/panaderia.db" >nul
if errorlevel 1 (
    echo ADVERTENCIA: NO se encontro plantilla/panaderia.db dentro del programa.
    echo En una computadora nueva la app no podra crear su base de datos.
    echo Copia panaderia.db a src\main\resources\plantilla\ y vuelve a ejecutar.
    pause
) else (
    echo OK: la plantilla esta incluida.
)

echo.
echo [3/5] Preparando la carpeta "paquete"...
if exist paquete rmdir /s /q paquete
mkdir paquete
copy /y "target\%JAR%" paquete\ >nul
if errorlevel 1 (
    echo ERROR al copiar el .jar
    pause
    exit /b 1
)

echo.
echo [4/5] Borrando la version anterior y generando la aplicacion...
if exist "dist\%APP%" rmdir /s /q "dist\%APP%"
if exist "dist\%APP%" (
    echo ERROR: no se pudo borrar dist\%APP%. Cierra la aplicacion y vuelve a intentar.
    pause
    exit /b 1
)

"%JAVA_HOME%\bin\jpackage.exe" --type app-image --name "%APP%" --input paquete --main-jar %JAR% --main-class com.mycompany.panaderia.local.Launcher --dest dist %OPCION_ICONO%
if errorlevel 1 (
    echo.
    echo ERROR al generar la aplicacion con jpackage.
    pause
    exit /b 1
)

echo.
echo [5/5] Creando el .zip para compartir...
if exist "dist\%APP%.zip" del /q "dist\%APP%.zip"
powershell -NoProfile -Command "Compress-Archive -Path 'dist\%APP%' -DestinationPath 'dist\%APP%.zip' -Force"
if errorlevel 1 (
    echo ADVERTENCIA: no se pudo crear el .zip. Puedes comprimir la carpeta a mano.
)

echo.
echo ============================================================
echo  LISTO
echo  Aplicacion : dist\%APP%\%APP%.exe
echo  Para enviar: dist\%APP%.zip
echo ============================================================
explorer "dist"
pause
endlocal
