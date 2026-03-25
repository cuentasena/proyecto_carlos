@echo off
chcp 65001 >nul
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
  echo No se encontró "java". Instala JDK 17 y añádelo al PATH.
  echo Descarga: https://adoptium.net/
  pause
  exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
  echo No se encontró "mvn" ^(Maven^) en el PATH.
  echo.
  echo Opciones:
  echo   - Instala Maven y reinicia la terminal: https://maven.apache.org/install.html
  echo   - O abre esta carpeta en IntelliJ / Eclipse / VS Code con extensión Java y ejecuta la clase App.java
  echo.
  pause
  exit /b 1
)

echo Compilando y arrancando el servidor en http://localhost:8080
echo Cierra esta ventana para parar el servidor.
echo.
mvn -q compile exec:java
if errorlevel 1 pause
