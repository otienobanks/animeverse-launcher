@echo off
rem Lists every Home app on the TV so you know the stock launcher package name.
set /p TV=TV address IP:PORT: 
adb connect %TV%
adb -s %TV% shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
pause
