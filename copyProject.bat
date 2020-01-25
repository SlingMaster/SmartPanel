CLS
@echo off
FOR %%a IN (.) DO SET currentfolder=%%~na
@echo =
@echo ==============================================
@echo * Create Copy  %currentfolder% Project *
@echo ==============================================

@echo off
set DD=%DATE%
set TARGET_PATH=G:\ActiveProject
set PROJECT=%currentfolder%
set DIR_PROJECT=%PROJECT%

@echo ==============================================
@echo 	*** Del Temp Files ***
@echo ==============================================

rd .gradle /s /q
md .gradle
rd build /s /q
md build
rd app\build /s /q
md app\build

if not "%USERNAME%"=="Alex Dovby" goto :next

md %TARGET_PATH%
md %TARGET_PATH%\%DIR_PROJECT%
md %TARGET_PATH%\%DIR_PROJECT%\%DIR_PROJECT%%DATE%
rem md %TARGET_PATH%\%PROJECT%\%DIR_PROJECT%

rem xcopy %CD% %TARGET_PATH%\%PROJECT%\%DIR_PROJECT% /s /e /y /d
cd..
xcopy %PROJECT% %TARGET_PATH%\%DIR_PROJECT%\%DIR_PROJECT%%DATE% /s /e /y /d
@echo ==============================================
@echo Creation of Data is completed
@echo ==============================================
pause 
exit

:next
@echo =
@echo ==============================================
@echo 	*** Only for lazy ***
@echo ==============================================

pause 
exit