@echo off
setlocal

set PROJECT_DIR=C:\Users\admin\IdeaProjects\trumpcards

echo ===============================
echo  Rebuild project...
echo ===============================
cd /d "%PROJECT_DIR%"
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
call "%PROJECT_DIR%\desktop-libgdx\build\install\desktop-libgdx\bin\desktop-libgdx.bat"

endlocal