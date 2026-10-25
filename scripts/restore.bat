@echo off
rem Re-enables everything listed in debloat.txt.
cd /d "%~dp0"
set /p TV=TV address IP:PORT: 
adb connect %TV%
for /f "usebackq eol=# tokens=*" %%P in ("debloat.txt") do (
  echo Enabling %%P
  adb -s %TV% shell pm enable %%P
)
pause
