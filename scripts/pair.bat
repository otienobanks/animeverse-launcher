@echo off
rem Android 14 wireless debugging: one-time pairing.
rem TV: Settings > System > Developer options > Wireless debugging > Pair device with pairing code
where adb >nul 2>nul || (echo adb not found. Install Android platform-tools and add it to PATH. & pause & exit /b 1)
set /p PAIR=Pairing IP:PORT shown on the TV: 
adb pair %PAIR%
pause
