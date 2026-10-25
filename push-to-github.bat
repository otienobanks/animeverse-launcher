@echo off
rem Run once from this folder (needs Git for Windows). Pushes the project; GitHub then builds the APK.
cd /d "%~dp0"
git init -b main
git add -A
git commit -m "ANIMEVERSE launcher"
git remote add origin https://github.com/otienobanks/animeverse-launcher.git
git push -u origin main
pause
