@echo off
setlocal
cd /d "%~dp0.."
where adb >nul 2>nul || (echo adb not found. Install Android platform-tools and add it to PATH. & pause & exit /b 1)

set /p TV=TV address IP:PORT (shown under Wireless debugging, e.g. 192.168.1.50:37123): 
adb connect %TV%

set APK=app\build\outputs\apk\release\app-release.apk
if not exist "%APK%" set APK=animeverse.apk
if not exist "%APK%" (
  echo APK not found. Put the downloaded APK here as animeverse.apk
  pause & exit /b 1
)

echo Installing...
adb -s %TV% install -r "%APK%" || (pause & exit /b 1)

rem First launch creates the wallpaper folder on the TV.
adb -s %TV% shell am start -n com.animeverse.launcher/.MainActivity
timeout /t 3 >nul

if exist wallpaper.jpg (
  echo Pushing wallpaper.jpg ...
  adb -s %TV% push wallpaper.jpg /sdcard/Android/data/com.animeverse.launcher/files/wallpaper.jpg
  adb -s %TV% shell am force-stop com.animeverse.launcher
  adb -s %TV% shell am start -n com.animeverse.launcher/.MainActivity
)

echo Setting ANIMEVERSE as the Home app...
adb -s %TV% shell cmd role add-role-holder --user 0 android.app.role.HOME com.animeverse.launcher

echo.
echo Done. Press the Home button on the remote. If the TV asks which launcher to use,
echo choose ANIMEVERSE and tap "Always".
pause
