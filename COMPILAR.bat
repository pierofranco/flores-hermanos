@echo off
cd /d "%~dp0codigo"
call mvn -DskipTests package
if errorlevel 1 goto error
copy /y "target\flores-hermanos-1.0.0.jar" "..\flores-hermanos.jar"
echo Compilacion terminada. Ejecute INICIAR.bat.
pause
exit /b 0
:error
echo No se pudo compilar. Revise el registro y su instalacion de Java y Maven.
pause
exit /b 1
