@echo off
setlocal EnableExtensions EnableDelayedExpansion

title RecMobiSim - Compilacion del JAR

rem ============================================================
rem  GenerateJar.bat
rem ============================================================

cd /d "%~dp0.."
if errorlevel 1 (
    echo [ERROR] No se pudo acceder al directorio raiz del proyecto.
    echo         Comprueba la ubicacion del script.
    exit /b 1
)

set "ANT_OPTS=-Dfile.encoding=iso-8859-1"
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ============================================================
echo   RecMobiSim - Compilacion del proyecto
echo ============================================================
echo.
echo [INFO] Directorio del proyecto: %CD%
echo [INFO] JAVA_HOME: %JAVA_HOME%
echo.

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] No se ha encontrado java.exe en:
    echo         %JAVA_HOME%\bin\java.exe
    exit /b 1
)

where ant >NUL 2>&1
if errorlevel 1 (
    echo [ERROR] Apache Ant no esta instalado o no es accesible desde PATH.
    exit /b 1
)

if not exist "build.xml" (
    echo [ERROR] No se ha encontrado build.xml en:
    echo         %CD%
    exit /b 1
)

echo [INFO] Version de Java detectada:
java -version
if errorlevel 1 (
    echo [ERROR] No se pudo ejecutar Java correctamente.
    exit /b 1
)

echo.
echo [INFO] Version de Ant detectada:
call ant -version
if errorlevel 1 (
    echo [ERROR] No se pudo ejecutar Ant correctamente.
    exit /b 1
)

echo.
echo === Limpieza segura de cache de SQLite ===
if exist "%TEMP%\libsqlitejdbc.so" del /f /q "%TEMP%\libsqlitejdbc.so" 2>NUL
if exist "%TEMP%\sqlitejdbc.dll" del /f /q "%TEMP%\sqlitejdbc.dll" 2>NUL
if exist "%TEMP%\sqlitejdbc64.dll" del /f /q "%TEMP%\sqlitejdbc64.dll" 2>NUL

echo.
echo === Eliminando directorio bin\ ===
if exist "bin" (
    rd /s /q "bin" 2>NUL
    if exist "bin" (
        echo [WARN] No se pudo eliminar completamente bin\.
    ) else (
        echo [OK] Directorio bin\ eliminado correctamente.
    )
) else (
    echo [INFO] El directorio bin\ no existe.
)

echo.
echo === Ejecutando Ant: clean ===
call ant clean
if errorlevel 1 (
    echo [ERROR] El target clean ha fallado.
    exit /b 1
)

echo.
echo === Ejecutando Ant: build ===
call ant build
if errorlevel 1 (
    echo [ERROR] El target build ha fallado.
    exit /b 1
)

echo.
echo === Ejecutando Ant: jar ===
call ant jar
if errorlevel 1 (
    echo [ERROR] El target jar ha fallado.
    exit /b 1
)

echo.
if exist "dist\RecMobiSim.jar" (
    echo [OK] Compilacion completada correctamente.
    echo [OK] JAR generado en: %CD%\dist\RecMobiSim.jar
    exit /b 0
) else (
    echo [WARN] La compilacion termino, pero no se encontro dist\RecMobiSim.jar
    echo        Revisa build.xml por si el JAR se genera en otra ruta.
    exit /b 0
)

goto :eof

:safe_delete_dir
set "TARGET_BASE=%~1"
set "TARGET_MASK=%~2"
if "%TARGET_BASE%"=="" goto :eof
if not exist "%TARGET_BASE%" goto :eof
for /d %%D in ("%TARGET_BASE%\%TARGET_MASK%") do (
    if exist "%%~fD" (
        rd /s /q "%%~fD" 2>NUL
        if exist "%%~fD" (
            echo [WARN] No se pudo eliminar el directorio: %%~fD
        ) else (
            echo [OK] Directorio eliminado: %%~fD
        )
    )
)
goto :eof

:safe_delete_file
set "TARGET_FILE=%~1"
if "%TARGET_FILE%"=="" goto :eof
if exist "%TARGET_FILE%" (
    del /f /q "%TARGET_FILE%" 2>NUL
    if exist "%TARGET_FILE%" (
        echo [WARN] No se pudo eliminar el fichero: %TARGET_FILE%
    ) else (
        echo [OK] Fichero eliminado: %TARGET_FILE%
    )
)
goto :eof