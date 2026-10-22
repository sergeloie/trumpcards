@echo off
setlocal

rem The script works in any checkout: %~dp0 is the directory the script is in.
cd /d "%~dp0"

echo ===============================
echo  Rebuild project...
echo ===============================
call gradlew.bat :desktop-libgdx:installDist

if errorlevel 1 (
    echo.
    echo Build failed. App will not be launched
    pause
    exit /b 1
)

echo.
echo ===============================
echo  Launching app...
echo ===============================
call "%~dp0desktop-libgdx\build\install\desktop-libgdx\bin\desktop-libgdx.bat"

endlocal