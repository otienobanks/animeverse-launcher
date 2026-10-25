@echo off
rem Saves installed package lists to packages-*.txt so you can pick what to disable.
set /p TV=TV address IP:PORT: 
adb connect %TV%
adb -s %TV% shell pm list packages -3 > packages-user.txt
adb -s %TV% shell pm list packages -s > packages-system.txt
echo Saved packages-user.txt and packages-system.txt
pause
