@echo off
setlocal enabledelayedexpansion
rem Disables (NOT uninstalls) every package listed in debloat.txt. Fully reversible with restore.bat.
cd /d "%~dp0"
set /p TV=TV address IP:PORT: 
adb connect %TV%
for /f "usebackq eol=# tokens=*" %%P in ("debloat.txt") do (
  echo Disabling %%P
  adb -s %TV% shell pm disable-user --user 0 %%P
)
pause
